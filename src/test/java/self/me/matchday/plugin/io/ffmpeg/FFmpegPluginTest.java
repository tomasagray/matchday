/*
 * Copyright (c) 2020.
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

package self.me.matchday.plugin.io.ffmpeg;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.CreateTestData;
import self.me.matchday.util.Log;
import self.me.matchday.util.RecursiveDirectoryDeleter;
import self.me.matchday.util.ResourceFileReader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for FFmpeg plugin")
@TestMethodOrder(OrderAnnotation.class)
class FFmpegPluginTest {

  public static final int MIN_EXPECTED_FILE_COUNT = 100;
  private static final String LOG_TAG = "FFmpegPluginTest";
  // Test constants
  private static final String STORAGE_LOCATION = "src/test/data/video_test";
  private static final String SAMPLE_METADATA_JSON = "ffprobe_sample_metadata.json";
  // Test resources
  private static File storageLocation;
  private static FFmpegPlugin ffmpegPlugin;
  private static FFmpegMetadata expectedMetadata;
  private static List<URL> testUrls;

  @BeforeAll
  static void setUp(@Autowired FFmpegPlugin plugin) throws IOException {

    // Get FFMPEG instance
    ffmpegPlugin = plugin;

    try {
      // Get video storage location
      storageLocation = new File(STORAGE_LOCATION);
      Log.i(LOG_TAG, "Got storage path: " + storageLocation.getAbsolutePath());

      // Create directory if not exists
      if (!storageLocation.exists()) {
        final boolean mkdir = storageLocation.mkdir();
        Log.i(LOG_TAG, String.format("Created directory: [%s] ? : %s", storageLocation, mkdir));
      }

      // initialize test URI
      final URL preMatchUrl = CreateTestData.getPreMatchUrl();
      final URL firstHalfUrl = CreateTestData.getFirstHalfUrl();
      final URL secondHalfUrl = CreateTestData.getSecondHalfUrl();
      final URL postMatchUrl = CreateTestData.getPostMatchUrl();

      assertThat(preMatchUrl).isNotNull();
      assertThat(firstHalfUrl).isNotNull();
      assertThat(secondHalfUrl).isNotNull();
      assertThat(postMatchUrl).isNotNull();

      testUrls = List.of(preMatchUrl, firstHalfUrl, secondHalfUrl, postMatchUrl);

    } catch (NullPointerException e) {
      throw new IOException("Error reading test video storage location!", e);
    }

    // Read test metadata & deserialize
    List<String> sampleMetadata =
        ResourceFileReader.readTextResource(FFprobeTest.class, SAMPLE_METADATA_JSON);
    // Parse JSON to object
    expectedMetadata = new Gson().fromJson(String.join(" ", sampleMetadata), FFmpegMetadata.class);
  }

  @AfterEach
  void tearDown() throws InterruptedException {

    // Ensure all streaming tasks are stopped
    ffmpegPlugin.interruptAllStreamTasks();
    // Give time for tasks to die
    Thread.sleep(5_000L);

    try {
      // Clean up files generated by testing
      Log.i(LOG_TAG, "Cleaning up test data...");
      Files.walkFileTree(storageLocation.toPath(), new RecursiveDirectoryDeleter());
    } catch (IOException e) {
      Log.e(LOG_TAG, "Error during test cleanup: " + e.getMessage());
    }
  }

  @Test
  @Order(1)
  @DisplayName("Test concat protocol stream remote data to correct path in required time")
  void testConcatStreamUris() throws InterruptedException {

    final List<URI> testUris = getTestUris();
    final FFmpegStreamTask streamTask = ffmpegPlugin.streamUris(testUris, storageLocation.toPath());
    final Path streamingPath = streamTask.getOutputPath();
    // Start stream
    streamTask.start();
    Log.i(
        LOG_TAG,
        String.format(
            "Began streaming to path: [%s] at time: %s",
            streamingPath.toAbsolutePath(), Instant.now()));

    // Wait for stream, then get file count
    int actualFileCount = getActualFileCount(streamingPath);
    // perform test
    assertThat(actualFileCount).isGreaterThanOrEqualTo(MIN_EXPECTED_FILE_COUNT);
  }

  @Test
  @Order(2)
  @DisplayName("Test single HLS stream generation")
  void testSingleHlsStream() throws InterruptedException {

    final FFmpegStreamTask streamTask =
        ffmpegPlugin.streamUri(getTestUris().get(1), storageLocation.toPath());
    final Path streamingPath = streamTask.getOutputPath();
    Log.i(LOG_TAG, "Starting test stream to: " + streamingPath.toAbsolutePath());

    // Start stream
    final ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.submit(streamTask);

    // Run test
    final int actualFileCount = getActualFileCount(streamingPath);
    Log.i(LOG_TAG, "Actual file count: " + actualFileCount);
    assertThat(actualFileCount).isGreaterThanOrEqualTo(MIN_EXPECTED_FILE_COUNT);
  }

  @Test
  @Order(3)
  @DisplayName("Verify that all previously started streaming tasks can be successfully interrupted")
  void interruptAllStreamTasks() {

    final int streamingTaskCount = ffmpegPlugin.getStreamingTaskCount();
    Log.i(
        LOG_TAG,
        String.format(
            "Attempting to interrupt %s streaming tasks at time: %s",
            streamingTaskCount, LocalDateTime.now()));
    ffmpegPlugin.interruptAllStreamTasks();

    final int actualStreamingTaskCount = ffmpegPlugin.getStreamingTaskCount();
    assertThat(actualStreamingTaskCount).isEqualTo(0);
  }

  @Test
  @Order(4)
  @DisplayName("Validate that a particular streaming task can be interrupted")
  void killStreamingTask() throws InterruptedException {

    // Start a new task
    final FFmpegStreamTask streamingTask =
        ffmpegPlugin.streamUris(getTestUris(), storageLocation.toPath());
    Log.i(LOG_TAG, "Created streaming task to: " + streamingTask.getOutputPath());
    streamingTask.start();
    // Wait...
    Thread.sleep(5_000L);
    Log.i(LOG_TAG, "Ensuring task has been started...");
    assertThat(ffmpegPlugin.getStreamingTaskCount()).isGreaterThan(0);

    Log.i(LOG_TAG, "Interrupting streaming task...");
    ffmpegPlugin.interruptStreamingTask(storageLocation.toPath());
    assertThat(ffmpegPlugin.getStreamingTaskCount()).isEqualTo(0);
  }

  @Test
  @Order(5)
  @DisplayName("Validate reading & parsing of remote video file metadata")
  void readFileMetadata() throws IOException {

    final URI uri = getTestUris().get(1);
    // Remove random seed
    final String noQuery = uri.toString().replaceAll("\\?rSeed=\\w*", "");
    final URI testUri = URI.create(noQuery);

    final FFmpegMetadata actualMetadata = ffmpegPlugin.readFileMetadata(testUri);
    Log.i(LOG_TAG, "Validating metadata read from file at URL: " + testUri);

    assertThat(actualMetadata).isEqualTo(expectedMetadata);
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

  private int getActualFileCount(Path streamingPath) throws InterruptedException {
    // Test data
    int actualFileCount = 0;

    final Duration timeout = Duration.ofSeconds(30);
    boolean filesFound = false;
    Duration elapsed = Duration.ZERO;
    Instant start = Instant.now();

    // Check file count
    while (!filesFound && elapsed.compareTo(timeout) < 0) {

      // Wait for FFMPEG to do its thing...
      Thread.sleep(30_000L);

      actualFileCount = streamingPath.getParent().toFile().list().length;
      Log.i(LOG_TAG, String.format("Found %s files on current sleep cycle...", actualFileCount));
      if (actualFileCount >= MIN_EXPECTED_FILE_COUNT) {
        Log.i(LOG_TAG, "That's enough for testing...");
        filesFound = true;
      }
      elapsed = Duration.between(start, Instant.now());
    }
    return actualFileCount;
  }
}
