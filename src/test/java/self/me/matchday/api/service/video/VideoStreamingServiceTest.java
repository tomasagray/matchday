/*
 * Copyright (c) 2022.
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.TestDataCreator;
import self.me.matchday.api.service.FileServerPluginService;
import self.me.matchday.api.service.FileServerUserService;
import self.me.matchday.model.Event;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.model.video.*;
import self.me.matchday.plugin.fileserver.TestFileServerPlugin;

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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class VideoStreamingServiceTest {

  private static final Logger logger = LogManager.getLogger(VideoStreamingServiceTest.class);

  private static final long waitSeconds = 10;

  // Service dependencies
  private static VideoStreamingService streamingService;
  private static VideoStreamLocatorPlaylistService locatorPlaylistService;
  private static VideoStreamLocatorService streamLocatorService;

  // Test data
  private static Event testMatch;
  private static VideoFile testVideoFile;
  private static VideoFileSource testFileSource;
  private static VideoStreamLocatorPlaylist deletablePlaylist;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull TestDataCreator testDataCreator,
      @Autowired @NotNull VideoStreamingService streamingService,
      @Autowired @NotNull VideoStreamLocatorPlaylistService locatorPlaylistService,
      @Autowired @NotNull VideoStreamLocatorService streamLocatorService,
      @Autowired @NotNull FileServerPluginService fileServerPluginService,
      @Autowired @NotNull FileServerUserService userService,
      @Autowired @NotNull TestFileServerPlugin testFileServerPlugin) {

    VideoStreamingServiceTest.streamingService = streamingService;
    VideoStreamingServiceTest.streamLocatorService = streamLocatorService;
    VideoStreamingServiceTest.locatorPlaylistService = locatorPlaylistService;

    // Create test user & login
    FileServerUser testFileServerUser = testDataCreator.createTestFileServerUser();
    userService.login(testFileServerUser);
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

    logger.info("Fetching event with ID: " + testMatch.getEventId());
    final Optional<Collection<VideoFileSource>> fileSrcOptional =
        streamingService.fetchVideoFileSources(testMatch.getEventId());
    assertThat(fileSrcOptional).isPresent();

    final Collection<VideoFileSource> actualFileSources = fileSrcOptional.get();
    logger.info("Retrieved files sources: " + actualFileSources);

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
    logger.info("Testing with Match:\n" + testMatch);
    logger.info(
        "Testing video stream creation with Event ID: {}, File Source ID: {}",
        testEventId,
        testFileSrcId);

    final Optional<VideoPlaylist> playlistOptional =
        streamingService.getVideoStreamPlaylist(testEventId, testFileSrcId, renderer);
    assertThat(playlistOptional).isPresent();
    final VideoPlaylist videoPlaylist = playlistOptional.get();
    logger.info("Retrieved VideoPlaylist: " + videoPlaylist);

    assertThat(videoPlaylist).isNotNull();
    assertThat(videoPlaylist.getPlaylist()).isNull();
    long recheckDelay = videoPlaylist.getWaitMillis();
    assertThat(recheckDelay).isGreaterThan(0);
    logger.info("VideoStreamingService returned a \"wait\" playlist, as expected...");

    VideoPlaylist testPlaylistOutput = null;
    while (recheckDelay > 0) {
      logger.info("Waiting {} milliseconds from playlist recommendation...", recheckDelay);
      Thread.sleep(recheckDelay);

      final Optional<VideoPlaylist> afterDelayStreamPlaylist =
          streamingService.getVideoStreamPlaylist(testEventId, testFileSrcId, renderer);
      assertThat(afterDelayStreamPlaylist).isNotNull().isPresent();
      testPlaylistOutput = afterDelayStreamPlaylist.get();
      assertThat(testPlaylistOutput).isNotNull();
      recheckDelay = testPlaylistOutput.getWaitMillis();
    }
    logger.info("Done waiting, performing recheck...");
    assertThat(testPlaylistOutput).isNotNull();
    final String renderedPlaylist = testPlaylistOutput.getPlaylist();
    logger.info("Test rendered M3U playlist:\n" + renderedPlaylist);
    assertThat(renderedPlaylist).isNotNull().isNotEmpty().isNotBlank();
    assertThat(testPlaylistOutput.getWaitMillis()).isEqualTo(0);

    final Optional<VideoStreamLocatorPlaylist> deleteOptional =
        locatorPlaylistService.getVideoStreamPlaylistFor(testFileSrcId);
    assertThat(deleteOptional).isPresent();
    VideoStreamingServiceTest.deletablePlaylist = deleteOptional.get();
  }

  @Test
  @Order(3)
  @DisplayName("Validate reading playlist file from disk")
  void readPlaylistFile() throws InterruptedException {

    final int MIN_PLAYLIST_LEN = 100;

    logger.info("Waiting {} seconds to ensure stream has started...", waitSeconds);
    TimeUnit.SECONDS.sleep(waitSeconds);
    logger.info("Done waiting. Proceeding with test...");

    final VideoStreamLocator testStreamLocator = getTestStreamLocator();
    logger.info(
        "Testing playlist file reading with File Source ID: {}, Stream Locator ID: {}",
        VideoStreamingServiceTest.testFileSource.getFileSrcId(),
        testStreamLocator.getStreamLocatorId());

    // Read test playlist file
    final Optional<String> actualPlaylistFile =
        streamingService.readPlaylistFile(testStreamLocator.getStreamLocatorId());

    // Perform tests
    assertThat(actualPlaylistFile).isNotNull().isNotEmpty().isPresent();
    final String actualPlaylistData = actualPlaylistFile.get();
    final int actualPlaylistSize = actualPlaylistData.getBytes(StandardCharsets.UTF_8).length;
    logger.info("Read playlist data:\n" + actualPlaylistData);
    logger.info("Data length: " + actualPlaylistSize);
    logger.info("Ensuring read playlist is longer than {} bytes", MIN_PLAYLIST_LEN);
    assertThat(actualPlaylistSize).isGreaterThan(MIN_PLAYLIST_LEN);
  }

  @Test
  @Order(4)
  @DisplayName("Validate reading of video segment (.ts) from disk")
  void getVideoSegmentResource() throws IOException {

    final long minContentLength = 500_000L;

    final VideoStreamLocator testStreamLocator = getTestStreamLocator();

    // Test params
    final Long testStreamLocatorId = testStreamLocator.getStreamLocatorId();
    final UUID testVideoFileSrcId = VideoStreamingServiceTest.testFileSource.getFileSrcId();
    final UUID testEventId = testMatch.getEventId();
    final String segmentId = "segment_00001";

    logger.info(
        "Testing video segment reading with Event ID: {}, File Source ID: {}, "
            + "Stream Locator ID: {}, Segment ID: {}",
        testEventId,
        testVideoFileSrcId,
        testStreamLocatorId,
        segmentId);
    // Read video resource
    final Resource actualVideoResource =
        streamingService.getVideoSegmentResource(testStreamLocatorId, segmentId);

    logger.debug("Read video segment: " + actualVideoResource);
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

    logger.info("Attempting to kill all tasks...");
    final int actualTasksKilled = streamingService.killAllStreamingTasks();
    logger.info("Waiting {} seconds for streams to die...", waitSeconds);
    TimeUnit.SECONDS.sleep(waitSeconds);

    logger.info("Service killed: {} tasks", actualTasksKilled);
    assertThat(actualTasksKilled).isGreaterThanOrEqualTo(expectedTasksKilled);
  }

  @Test
  @Order(6)
  @DisplayName("Validate ability to delete previously downloaded video data")
  void deleteVideoData() throws IOException, InterruptedException {

    assertThat(deletablePlaylist).isNotNull();
    final List<VideoStreamLocator> streamLocators = deletablePlaylist.getStreamLocators();

    logger.info("Waiting {} seconds to ensure streaming processes are dead...", waitSeconds);
    streamingService.killAllStreamingTasks();
    Thread.sleep(waitSeconds * 1_000);

    logger.info("Deleting video data...");
    streamingService.deleteVideoData(deletablePlaylist);

    // Validate data has been removed
    streamLocators.forEach(
        streamLocator -> {
          final Path playlistPath = streamLocator.getPlaylistPath();
          final boolean exists = playlistPath.toFile().exists();
          logger.info("Path: {} exists? {}", playlistPath, exists);
          assertThat(exists).isFalse();
        });
  }

  @Test
  @Order(7)
  @DisplayName("Validate a specific streaming task can be killed")
  public void killStreamingFor() throws InterruptedException, IOException {

    final int waitSeconds = 10;

    final VideoFileSource testFileSource = getTestFileSource();
    final UUID fileSrcId = testFileSource.getFileSrcId();
    logger.info(
        "Beginning test stream for file source stream killing with file source ID: " + fileSrcId);

    final Optional<VideoPlaylist> playlistOptional =
        streamingService.getVideoStreamPlaylist(
            testMatch.getEventId(), fileSrcId, new M3uRenderer());
    assertThat(playlistOptional).isPresent();
    final VideoPlaylist videoPlaylist = playlistOptional.get();
    logger.info("Using playlist: " + videoPlaylist);

    logger.info("Waiting {} seconds for streams to get started...", waitSeconds);
    TimeUnit.SECONDS.sleep(waitSeconds);
    logger.info("Done waiting. Attempting to kill streams for file source: " + fileSrcId);
    streamingService.killStreamingFor(fileSrcId);
    logger.info("Waiting: {} seconds for streams to die...", waitSeconds);
    TimeUnit.SECONDS.sleep(waitSeconds);
    logger.info("Done waiting. Deleting test data...");
    streamingService.deleteVideoData(testFileSource.getFileSrcId());
    logger.info("Test data successfully deleted.");
  }
}
