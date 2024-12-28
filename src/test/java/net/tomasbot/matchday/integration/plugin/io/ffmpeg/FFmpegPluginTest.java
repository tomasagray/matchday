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

package net.tomasbot.matchday.integration.plugin.io.ffmpeg;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.tomasbot.ffmpeg_wrapper.metadata.FFmpegMetadata;
import net.tomasbot.ffmpeg_wrapper.request.SimpleTranscodeRequest;
import net.tomasbot.ffmpeg_wrapper.task.FFmpegStreamTask;
import net.tomasbot.matchday.TestDataCreator;
import net.tomasbot.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import net.tomasbot.matchday.util.JsonParser;
import net.tomasbot.matchday.util.RecursiveDirectoryDeleter;
import net.tomasbot.matchday.util.ResourceFileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for FFmpeg plugin")
class FFmpegPluginTest {

  private static final Logger logger = LogManager.getLogger(FFmpegPluginTest.class);

  // Test constants
  private static final int SLEEP_SECONDS = 5;
  private static final int MIN_EXPECTED_FILE_COUNT = 25;
  private static final String SAMPLE_METADATA_JSON = "data/ffprobe_sample_metadata.json";

  // Test resources
  private static final List<Path> cleanupPaths = new ArrayList<>();

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final FFmpegPlugin ffmpegPlugin;
  private final FFmpegMetadata expectedMetadata;
  private final List<URL> testUrls;

  @Autowired
  public FFmpegPluginTest(TestDataCreator testDataCreator, FFmpegPlugin plugin) throws IOException {
    this.ffmpegPlugin = plugin;
    testUrls = getTestUrls(testDataCreator);
    // Read test metadata & deserialize
    String sampleMetadata = ResourceFileReader.readTextResource(SAMPLE_METADATA_JSON);
    // Parse JSON to object
    expectedMetadata = JsonParser.fromJson(sampleMetadata, FFmpegMetadata.class);
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

  @AfterAll
  static void tearDown() {
    try {
      // Clean up files generated by testing
      for (Path cleanupDir : cleanupPaths) {
        logger.info("Cleaning up test data at {}...", cleanupDir);
        Files.walkFileTree(cleanupDir, new RecursiveDirectoryDeleter());
      }
    } catch (IOException e) {
      logger.error("Error during test cleanup: {}", e.getMessage(), e);
    }
  }

  private SimpleTranscodeRequest getTranscodeRequest(
      @NotNull String storagePrefix, @NotNull AtomicInteger exitCode) throws IOException {
    final Path storage = Files.createTempDirectory(storagePrefix);
    cleanupPaths.add(storage);
    Path playlist = storage.resolve("playlist.m3u8");

    return SimpleTranscodeRequest.builder()
        .from(getTestUris().get(0))
        .to(playlist)
        .onEvent(logger::info)
        .onError(logger::error)
        .onComplete(exitCode::lazySet)
        .logFile(FFmpegStreamTask.getDefaultLogFile())
        .build();
  }

  @Test
  @DisplayName("Test single HLS stream generation")
  void testSingleHlsStream() throws InterruptedException, IOException {
    // given
    final AtomicInteger exitCode = new AtomicInteger(-1);
    final SimpleTranscodeRequest transcodeRequest =
        getTranscodeRequest("FFmpegPluginTest_HLS_STREAM_", exitCode);
    final FFmpegStreamTask streamTask = ffmpegPlugin.streamUri(transcodeRequest);
    final Path streamingPath = streamTask.getRequest().getTo();
    logger.info("Starting test stream to: {}", streamingPath.toAbsolutePath());

    // Start stream
    executor.submit(streamTask);
    TimeUnit.SECONDS.sleep(SLEEP_SECONDS * 5);

    // Run test
    final int actualFileCount = getActualFileCount(streamingPath);

    logger.info("Actual file count: {}", actualFileCount);
    logger.info("Exit code: {}", exitCode.get());
    assertThat(actualFileCount).isGreaterThanOrEqualTo(MIN_EXPECTED_FILE_COUNT);
  }

  @Test
  @DisplayName("Verify that all previously started streaming tasks can be successfully interrupted")
  void interruptAllStreamTasks() throws InterruptedException, IOException {
    // given
    final AtomicInteger exitCode = new AtomicInteger(-1);
    final int expectedExitCode = 137;

    final SimpleTranscodeRequest transcodeRequest =
        getTranscodeRequest("FFmpegPluginTest_KILL_ALL_STREAMS_", exitCode);
    final FFmpegStreamTask streamTask = ffmpegPlugin.streamUri(transcodeRequest);
    logger.info("Beginning test stream to: {}", streamTask.getRequest().getTo());

    // Start stream
    executor.submit(streamTask);
    TimeUnit.SECONDS.sleep(10);

    final int streamingTaskCount = ffmpegPlugin.getStreamingTaskCount();
    assertThat(streamingTaskCount).isGreaterThan(0);
    logger.info(
        "Attempting to interrupt {} streaming tasks at time: {}",
        streamingTaskCount,
        LocalDateTime.now());
    ffmpegPlugin.interruptAllStreamTasks();
    TimeUnit.SECONDS.sleep(SLEEP_SECONDS);

    final int actualStreamingTaskCount = ffmpegPlugin.getStreamingTaskCount();
    assertThat(exitCode.get()).isEqualTo(expectedExitCode);
    assertThat(actualStreamingTaskCount).isZero();
  }

  @Test
  @DisplayName("Validate that a particular streaming task can be interrupted")
  void killStreamingTask() throws InterruptedException, IOException {
    // given
    final AtomicInteger exitCode = new AtomicInteger(-1);
    final int expectedExitCode = 137;

    // Start a new task
    SimpleTranscodeRequest transcodeRequest =
        getTranscodeRequest("FFmpegPluginTest_KILL_STREAM_", exitCode);
    final FFmpegStreamTask streamingTask = ffmpegPlugin.streamUri(transcodeRequest);
    Path playlistPath = streamingTask.getRequest().getTo();
    logger.info("Created streaming task to: {}", playlistPath);
    executor.submit(streamingTask);

    // Wait...
    TimeUnit.SECONDS.sleep(SLEEP_SECONDS);
    logger.info("Ensuring task has been started...");
    assertThat(ffmpegPlugin.getStreamingTaskCount()).isGreaterThan(0);

    logger.info("Interrupting streaming task at: {} ...", playlistPath);
    ffmpegPlugin.interruptStreamingTask(playlistPath);

    // Allow time to die...
    TimeUnit.SECONDS.sleep(SLEEP_SECONDS);
    assertThat(exitCode.get()).isEqualTo(expectedExitCode);
    assertThat(ffmpegPlugin.getStreamingTaskCount()).isZero();
  }

  @Test
  @DisplayName("Validate reading & parsing of remote video file metadata")
  void readFileMetadata() throws IOException {
    final URI uri = getTestUris().get(1);
    // Remove random seed
    final String noQuery = uri.toString().replaceAll("\\?rSeed=[\\w-]*", "");
    final URI testUri = URI.create(noQuery);

    final FFmpegMetadata actualMetadata = ffmpegPlugin.readFileMetadata(testUri);
    assertThat(actualMetadata).isEqualTo(expectedMetadata);
    logger.info("The data matches!");
  }

  @Test
  @DisplayName("Validate versions of ffmpeg & ffprobe")
  void testVersions() throws IOException {
    // given
    final Pattern versionPattern = Pattern.compile("[\\w\\s.]+");

    // when
    String ffmpegVersion = ffmpegPlugin.getFFmpegVersion();
    String ffprobeVersion = ffmpegPlugin.getFFprobeVersion();

    logger.info("Found ffmpeg version: {}", ffmpegVersion);
    logger.info("Found ffprobe version: {}", ffprobeVersion);

    // then
    assertThat(ffmpegVersion).isNotNull().isNotEmpty();
    assertThat(ffprobeVersion).isNotNull().isNotEmpty();
    assertThat(versionPattern.matcher(ffmpegVersion).find()).isTrue();
    assertThat(versionPattern.matcher(ffprobeVersion).find()).isTrue();
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
