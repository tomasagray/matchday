/*
 * Copyright (c) 2023.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.integration.plugin.io.ffmpeg;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import self.me.matchday.TestDataCreator;
import self.me.matchday.plugin.io.ffmpeg.FFmpegMetadata;
import self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import self.me.matchday.plugin.io.ffmpeg.FFmpegStreamTask;
import self.me.matchday.util.JsonParser;
import self.me.matchday.util.RecursiveDirectoryDeleter;
import self.me.matchday.util.ResourceFileReader;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for FFmpeg plugin")
@TestMethodOrder(OrderAnnotation.class)
class FFmpegPluginTest {

  private static final Logger logger = LogManager.getLogger(FFmpegPluginTest.class);

  // Test constants
  private static final int SLEEP_SECONDS = 5;
  private static final int MIN_EXPECTED_FILE_COUNT = 25;
  private static final String SAMPLE_METADATA_JSON = "data/ffprobe_sample_metadata.json";

  // Test resources
  private final FFmpegPlugin ffmpegPlugin;
  private final FFmpegMetadata expectedMetadata;
  private final List<URL> testUrls;
  private Path testPlaylist;

  @Value("${video-resources.file-storage-location}")
  private String storageLocation;

  @Autowired
  public FFmpegPluginTest(TestDataCreator testDataCreator, FFmpegPlugin plugin) throws IOException {
    this.ffmpegPlugin = plugin;
    testUrls = getTestUrls(testDataCreator);
    // Read test metadata & deserialize
    String sampleMetadata = ResourceFileReader.readTextResource(SAMPLE_METADATA_JSON);
    // Parse JSON to object
    expectedMetadata = JsonParser.fromJson(sampleMetadata, FFmpegMetadata.class);
  }

  @BeforeEach
  void setup() {
    if (testPlaylist == null) {
      this.testPlaylist = Path.of(storageLocation).resolve("playlist.m3u8");
    }
  }

  @NotNull
  private @Unmodifiable List<URL> getTestUrls(@NotNull TestDataCreator testDataCreator) {
    final List<URL> testUrls;
    // initialize test URI
    final URL preMatchUrl = testDataCreator.getPreMatchUrl();
    final URL firstHalfUrl = testDataCreator.getFirstHalfUrl();
    final URL secondHalfUrl = testDataCreator.getSecondHalfUrl();
    final URL postMatchUrl = testDataCreator.getPostMatchUrl();

    assertThat(preMatchUrl).isNotNull();
    assertThat(firstHalfUrl).isNotNull();
    assertThat(secondHalfUrl).isNotNull();
    assertThat(postMatchUrl).isNotNull();

    testUrls = List.of(preMatchUrl, firstHalfUrl, secondHalfUrl, postMatchUrl);
    return testUrls;
  }

  @AfterEach
  void tearDown() throws InterruptedException {
    // Ensure all streaming tasks are stopped
    ffmpegPlugin.interruptAllStreamTasks();
    // Give time for tasks to die
    TimeUnit.SECONDS.sleep(SLEEP_SECONDS);

    try {
      // Clean up files generated by testing
      logger.info("Cleaning up test data...");
      Path storage = Path.of(storageLocation);
      if (storage.toFile().exists()) {
        Files.walkFileTree(storage, new RecursiveDirectoryDeleter());
      }
    } catch (IOException e) {
      logger.error("Error during test cleanup: " + e.getMessage(), e);
    }
  }

  @Test
  @Order(1)
  @DisplayName("Test concat protocol streams remote data to the correct path within required time")
  void testConcatStreamUris() throws IOException {

    final List<URI> testUris = getTestUris();

    final FFmpegStreamTask streamTask =
        ffmpegPlugin.streamUris(testPlaylist, testUris.toArray(new URI[0]));
    final Path streamingPath = streamTask.getPlaylistPath();
    // Start stream
    final Process process = streamTask.execute();
    logger.info(
        "Began streaming to path: [{}] at time: {}", streamingPath.toAbsolutePath(), Instant.now());
    // attach for log output
    logOutput(process.getErrorStream());

    // Wait for stream, then get file count
    int actualFileCount = getActualFileCount(streamingPath);
    // perform test
    assertThat(actualFileCount).isGreaterThanOrEqualTo(MIN_EXPECTED_FILE_COUNT);
  }

  @Test
  @Order(2)
  @DisplayName("Test single HLS stream generation")
  void testSingleHlsStream() throws IOException {

    final FFmpegStreamTask streamTask = ffmpegPlugin.streamUris(testPlaylist, getTestUris().get(1));
    final Path streamingPath = streamTask.getPlaylistPath();
    logger.info("Starting test stream to: " + streamingPath.toAbsolutePath());

    // Start stream
    final Process process = streamTask.execute();
    logOutput(process.getErrorStream());
    // Run test
    final int actualFileCount = getActualFileCount(streamingPath);

    logger.info("Actual file count: " + actualFileCount);
    assertThat(actualFileCount).isGreaterThanOrEqualTo(MIN_EXPECTED_FILE_COUNT);
  }

  private void logOutput(@NotNull final InputStream is) {
    final Flux<String> emitter =
        Flux.using(
            () -> new BufferedReader(new InputStreamReader(is)).lines(),
            Flux::fromStream,
            Stream::close);
    emitter
        .takeUntil(s -> s.matches("[\\w\\s-./=]*time=00:01:[\\d.]+[\\w\\s-./=]*"))
        .doOnComplete(() -> logger.info("Test streaming complete."))
        .subscribe(line -> logger.info("[ffmpeg]: " + line));
  }

  @Test
  @Order(3)
  @DisplayName("Verify that all previously started streaming tasks can be successfully interrupted")
  void interruptAllStreamTasks() throws InterruptedException {

    final int streamingTaskCount = ffmpegPlugin.getStreamingTaskCount();
    logger.info(
        "Attempting to interrupt {} streaming tasks at time: {}",
        streamingTaskCount,
        LocalDateTime.now());
    ffmpegPlugin.interruptAllStreamTasks();
    TimeUnit.SECONDS.sleep(SLEEP_SECONDS);

    final int actualStreamingTaskCount = ffmpegPlugin.getStreamingTaskCount();
    assertThat(actualStreamingTaskCount).isEqualTo(0);
  }

  @Test
  @Order(4)
  @DisplayName("Validate that a particular streaming task can be interrupted")
  void killStreamingTask() throws InterruptedException, IOException {

    // Start a new task
    final FFmpegStreamTask streamingTask =
        ffmpegPlugin.streamUris(testPlaylist, getTestUris().toArray(new URI[0]));
    logger.info("Created streaming task to: " + streamingTask.getPlaylistPath());
    final Process process = streamingTask.execute();
    logOutput(process.getErrorStream());
    // Wait...
    TimeUnit.SECONDS.sleep(SLEEP_SECONDS);
    logger.info("Ensuring task has been started...");
    assertThat(ffmpegPlugin.getStreamingTaskCount()).isGreaterThan(0);

    logger.info("Interrupting streaming task...");
    ffmpegPlugin.interruptStreamingTask(streamingTask.getPlaylistPath());

    // Allow time to die...
    TimeUnit.SECONDS.sleep(SLEEP_SECONDS);
    assertThat(ffmpegPlugin.getStreamingTaskCount()).isEqualTo(0);
  }

  @Test
  @Order(5)
  @DisplayName("Validate reading & parsing of remote video file metadata")
  void readFileMetadata() throws IOException {

    final URI uri = getTestUris().get(1);
    // Remove random seed
    final String noQuery = uri.toString().replaceAll("\\?rSeed=[\\w-]*", "");
    final URI testUri = URI.create(noQuery);
    logger.info("Getting data from URL: " + testUri);

    final FFmpegMetadata actualMetadata = ffmpegPlugin.readFileMetadata(testUri);
    assertThat(actualMetadata).isEqualTo(expectedMetadata);
    logger.info("The data matches!");
  }

  private List<URI> getTestUris() {

    return testUrls.stream()
        .map(
            url -> {
              try {
                return url.toURI();
              } catch (URISyntaxException ignore) {
                return null;
              }
            })
        .collect(Collectors.toList());
  }

  private int getActualFileCount(@NotNull Path streamingPath) {

    final long waitSeconds = 30;

    // Test data
    int actualFileCount;

    // Wait for FFMPEG to do its thing...
    logger.info("Waiting {} seconds before retry...", waitSeconds);
    final String[] fileList = streamingPath.getParent().toFile().list();
    assertThat(fileList).isNotNull();
    actualFileCount = fileList.length;
    logger.info("Found {} files on current sleep cycle...", actualFileCount);
    return actualFileCount;
  }
}
