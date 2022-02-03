/*
 * Copyright (c) 2021.
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

package self.me.matchday.api.service.video;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday._DEVFIXTURES.plugin.TestFileServerPlugin;
import self.me.matchday.api.service.FileServerService;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.model.Match;
import self.me.matchday.model.video.*;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for video streaming service")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VideoStreamingServiceTest {

  private static final String LOG_TAG = "VideoStreamingServiceTest";

  private static final long waitSeconds = 10;

  // Service dependencies
  private static TestDataCreator testDataCreator;
  private static VideoStreamingService streamingService;
  private static VideoStreamLocatorPlaylistService locatorPlaylistService;

  // Test data
  private static Match testMatch;
  private static FileServerUser testFileServerUser;
  private static VideoStreamLocatorService streamLocatorService;
  private static VideoFile testVideoFile;
  private static VideoFileSource testFileSource;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull final TestDataCreator testDataCreator,
      @Autowired @NotNull final VideoStreamingService streamingService,
      @Autowired @NotNull final VideoStreamLocatorPlaylistService locatorPlaylistService,
      @Autowired @NotNull final VideoStreamLocatorService streamLocatorService,
      @Autowired @NotNull final FileServerService fileServerService,
      @Autowired @NotNull final TestFileServerPlugin testFileServerPlugin) {

    VideoStreamingServiceTest.testDataCreator = testDataCreator;
    VideoStreamingServiceTest.streamingService = streamingService;
    VideoStreamingServiceTest.streamLocatorService = streamLocatorService;
    VideoStreamingServiceTest.locatorPlaylistService = locatorPlaylistService;

    // Create test user & login
    testFileServerUser = testDataCreator.createTestFileServerUser();
    fileServerService.login(testFileServerUser, testFileServerPlugin.getPluginId());
    assertThat(testFileServerUser.isLoggedIn()).isTrue();

    // Create test data
    VideoStreamingServiceTest.testMatch = testDataCreator.createTestMatch();
    VideoStreamingServiceTest.testFileSource = getTestFileSource();
    VideoStreamingServiceTest.testVideoFile =
        testFileSource.getVideoFilePacks().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No VideoFilePacks for test data"))
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No VideoFiles for test data"));
  }

  @AfterAll
  static void tearDown() {
    Log.i(LOG_TAG, "Deleting test data: " + testMatch);
    // delete test data
    testDataCreator.deleteFileServerUser(testFileServerUser);
    testDataCreator.deleteTestEvent(testMatch);
  }

  @NotNull
  private static VideoFileSource getTestFileSource() {

    AtomicReference<VideoFileSource> atomicFileSource = new AtomicReference<>();
    final Set<VideoFileSource> fileSources = testMatch.getFileSources();
    fileSources.stream().findFirst().ifPresent(atomicFileSource::set);
    final VideoFileSource testFileSource = atomicFileSource.get();

    assertThat(testFileSource).isNotNull();
    return testFileSource;
  }

  @Test
  @Order(1)
  @DisplayName("Validate retrieval of file sources for a given Event")
  void fetchVideoFileSources() {

    Log.i(LOG_TAG, "Fetching event with ID: " + testMatch.getEventId());
    final Optional<Collection<VideoFileSource>> fileSrcOptional =
        streamingService.fetchVideoFileSources(testMatch.getEventId());
    assertThat(fileSrcOptional).isPresent();

    final Collection<VideoFileSource> actualFileSources = fileSrcOptional.get();
    Log.i(LOG_TAG, "Retrieved files sources: " + actualFileSources);

    final Set<VideoFileSource> expectedFileSources = testMatch.getFileSources();
    assertThat(actualFileSources.size()).isEqualTo(expectedFileSources.size());
    assertThat(actualFileSources).containsAll(expectedFileSources);
  }

  @Test
  @Order(2)
  @DisplayName("Test that a playlist is created & returned")
  void getVideoStreamPlaylist() throws Exception {

    final M3uRenderer renderer = new M3uRenderer();
    // test variables
    final UUID testEventId = testMatch.getEventId();
    final UUID testFileSrcId = testFileSource.getFileSrcId();
    Log.i(LOG_TAG, "Testing with Match:\n" + testMatch);
    Log.i(
        LOG_TAG,
        String.format(
            "Testing video stream creation with Event ID: %s, File Source ID: %s",
            testEventId, testFileSrcId));

    final Optional<VideoPlaylist> playlistOptional =
        streamingService.getVideoStreamPlaylist(testEventId, testFileSrcId, renderer);
    assertThat(playlistOptional).isPresent();
    final VideoPlaylist videoPlaylist = playlistOptional.get();
    Log.i(LOG_TAG, "Retrieved VideoPlaylist: " + videoPlaylist);

    assertThat(videoPlaylist).isNotNull();
    assertThat(videoPlaylist.getPlaylist()).isNull();
    long recheckDelay = videoPlaylist.getWaitMillis();
    assertThat(recheckDelay).isGreaterThan(0);
    Log.i(LOG_TAG, "VideoStreamingService returned a \"wait\" playlist, as expected...");

    VideoPlaylist testPlaylistOutput = null;
    while (recheckDelay > 0) {
      Log.i(
          LOG_TAG,
          String.format("Waiting %s milliseconds from playlist recommendation...", recheckDelay));
      Thread.sleep(recheckDelay);

      final Optional<VideoPlaylist> afterDelayStreamPlaylist =
          streamingService.getVideoStreamPlaylist(testEventId, testFileSrcId, renderer);
      assertThat(afterDelayStreamPlaylist).isNotNull().isPresent();
      testPlaylistOutput = afterDelayStreamPlaylist.get();
      assertThat(testPlaylistOutput).isNotNull();
      recheckDelay = testPlaylistOutput.getWaitMillis();
    }
    Log.i(LOG_TAG, "Done waiting, performing recheck...");
    assertThat(testPlaylistOutput).isNotNull();
    final String renderedPlaylist = testPlaylistOutput.getPlaylist();
    Log.i(LOG_TAG, "Test rendered M3U playlist:\n" + renderedPlaylist);
    assertThat(renderedPlaylist).isNotNull().isNotEmpty().isNotBlank();
    assertThat(testPlaylistOutput.getWaitMillis()).isEqualTo(0);
  }

  @Test
  @Order(3)
  @DisplayName("Validate reading playlist file from disk")
  void readPlaylistFile() throws InterruptedException {

    final int MIN_PLAYLIST_LEN = 100;

    Log.i(
        LOG_TAG, String.format("Waiting %d seconds to ensure stream has started...", waitSeconds));
    TimeUnit.SECONDS.sleep(waitSeconds);
    Log.i(LOG_TAG, "Done waiting. Proceeding with test...");

    final VideoStreamLocator testStreamLocator = getTestStreamLocator();
    Log.i(
        LOG_TAG,
        String.format(
            "Testing playlist file reading with File Source ID: %s, Stream Locator ID: %s",
            VideoStreamingServiceTest.testFileSource.getFileSrcId(),
            testStreamLocator.getStreamLocatorId()));

    // Read test playlist file
    final Optional<String> actualPlaylistFile =
        streamingService.readPlaylistFile(
            testMatch.getEventId(),
            VideoStreamingServiceTest.testFileSource.getFileSrcId(),
            testStreamLocator.getStreamLocatorId());

    // Perform tests
    assertThat(actualPlaylistFile).isNotNull().isNotEmpty().isPresent();
    final String actualPlaylistData = actualPlaylistFile.get();
    final int actualPlaylistSize = actualPlaylistData.getBytes(StandardCharsets.UTF_8).length;
    Log.i(LOG_TAG, "Read playlist data:\n" + actualPlaylistData);
    Log.i(LOG_TAG, "Data length: " + actualPlaylistSize);
    Log.i(
        LOG_TAG, String.format("Ensuring read playlist is longer than %d bytes", MIN_PLAYLIST_LEN));
    assertThat(actualPlaylistSize).isGreaterThan(MIN_PLAYLIST_LEN);
  }

  @Test
  @Order(4)
  @DisplayName("Validate reading of video segment (.ts) from disk")
  void getVideoSegmentResource() throws IOException {

    final int minContentLength = 500_000;

    final VideoStreamLocator testStreamLocator = getTestStreamLocator();

    // Test params
    final Long testStreamLocatorId = testStreamLocator.getStreamLocatorId();
    final UUID testVideoFileSrcId = VideoStreamingServiceTest.testFileSource.getFileSrcId();
    final UUID testEventId = testMatch.getEventId();
    final String segmentId = "segment_00001";

    Log.i(
        LOG_TAG,
        String.format(
            "Testing video segment reading with Event ID: %s, File Source ID: %s, Stream Locator ID: %s, Segment ID: %s",
            testEventId, testVideoFileSrcId, testStreamLocatorId, segmentId));
    // Read video resource
    final Resource actualVideoResource =
        streamingService.getVideoSegmentResource(
            testEventId, testVideoFileSrcId, testStreamLocatorId, segmentId);

    Log.d(LOG_TAG, "Read video segment: " + actualVideoResource);
    assertThat(actualVideoResource).isNotNull();
    assertThat(actualVideoResource.contentLength()).isGreaterThan(minContentLength);
  }

  @NotNull
  private VideoStreamLocator getTestStreamLocator() {
    final Optional<VideoStreamLocator> locatorOptional =
        streamLocatorService.getStreamLocatorFor(testVideoFile);
    assertThat(locatorOptional).isPresent();
    final VideoStreamLocator testStreamLocator = locatorOptional.get();
    assertThat(testStreamLocator).isNotNull();
    return testStreamLocator;
  }

  @Test
  @Order(5)
  @DisplayName("Ensure streaming service can kill all current streaming tasks")
  void killAllStreamingTasks() throws InterruptedException {

    final int waitSeconds = 10;
    final int expectedTasksKilled = 3;

    Log.i(LOG_TAG, "Attempting to kill all tasks...");
    final int actualTasksKilled = streamingService.killAllStreamingTasks();
    Log.i(LOG_TAG, String.format("Waiting %d seconds for streams to die...", waitSeconds));
    TimeUnit.SECONDS.sleep(waitSeconds);

    Log.i(LOG_TAG, String.format("Service killed: %d tasks", actualTasksKilled));
    assertThat(actualTasksKilled).isGreaterThanOrEqualTo(expectedTasksKilled);
  }

  @Test
  @Order(6)
  @DisplayName("Validate ability to delete previously downloaded video data")
  void deleteVideoData() throws IOException, InterruptedException {

    final Optional<VideoStreamLocatorPlaylist> playlistOptional =
        locatorPlaylistService.getVideoStreamPlaylistFor(testFileSource.getFileSrcId());
    assertThat(playlistOptional).isPresent();
    final VideoStreamLocatorPlaylist testVideoStreamLocatorPlaylist = playlistOptional.get();
    final List<VideoStreamLocator> streamLocators =
        testVideoStreamLocatorPlaylist.getStreamLocators();

    Log.i(
        LOG_TAG,
        String.format("Waiting %d seconds to ensure streaming processes are dead...", waitSeconds));
    streamingService.killAllStreamingTasks();
    Thread.sleep(waitSeconds * 1_000);

    Log.i(LOG_TAG, "Deleting video data...");
    streamingService.deleteVideoData(testVideoStreamLocatorPlaylist);

    // Validate data has been removed
    streamLocators.forEach(
        streamLocator -> {
          final Path playlistPath = streamLocator.getPlaylistPath();
          final boolean exists = playlistPath.toFile().exists();
          Log.i(LOG_TAG, String.format("Path: %s exists? %s", playlistPath, exists));
          assertThat(exists).isFalse();
        });
  }

  @Test
  @Disabled
  public void killStreamingFor() throws InterruptedException {

    final int waitSeconds = 10;

    final VideoFileSource testFileSource = getTestFileSource();
    final UUID fileSrcId = testFileSource.getFileSrcId();
    Log.i(
        LOG_TAG,
        "Beginning test stream for file source stream killing with file source ID: " + fileSrcId);

    final Optional<VideoPlaylist> playlistOptional =
        streamingService.getVideoStreamPlaylist(
            testMatch.getEventId(), fileSrcId, new M3uRenderer());
    assertThat(playlistOptional).isPresent();
    final VideoPlaylist videoPlaylist = playlistOptional.get();
    Log.i(LOG_TAG, "Using playlist: " + videoPlaylist);

    Log.i(LOG_TAG, String.format("Waiting %d seconds for streams to get started...", waitSeconds));
    TimeUnit.SECONDS.sleep(waitSeconds);

    Log.i(LOG_TAG, "Done waiting. Attempting to kill streams for file source: " + fileSrcId);
    streamingService.killStreamingFor(fileSrcId);
  }
}
