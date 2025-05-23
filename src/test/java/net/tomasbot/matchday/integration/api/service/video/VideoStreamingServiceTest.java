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

package net.tomasbot.matchday.integration.api.service.video;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import net.tomasbot.matchday.TestDataCreator;
import net.tomasbot.matchday.TestFileServerPlugin;
import net.tomasbot.matchday.api.service.FileServerPluginService;
import net.tomasbot.matchday.api.service.FileServerUserService;
import net.tomasbot.matchday.api.service.video.VideoStreamLocatorPlaylistService;
import net.tomasbot.matchday.api.service.video.VideoStreamLocatorService;
import net.tomasbot.matchday.api.service.video.VideoStreamingService;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.FileServerUser;
import net.tomasbot.matchday.model.video.*;
import net.tomasbot.matchday.util.RecursiveDirectoryDeleter;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for video streaming service")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(Lifecycle.PER_CLASS)
class VideoStreamingServiceTest {

  private static final Logger logger = LogManager.getLogger(VideoStreamingServiceTest.class);

  private static final int WAIT_SECONDS = 5;

  // Test data
  private static final List<Event> cleanupData = new ArrayList<>();
  private static final List<String> videoStorageDirs = new ArrayList<>();
  // Service dependencies
  private static VideoStreamingService streamingService;
  private static VideoFileSource testFileSource;
  private final VideoStreamLocatorPlaylistService playlistService;
  private final VideoStreamLocatorService streamLocatorService;

  private Event testMatch;
  private VideoFile testVideoFile;
  private int testStreamTaskCount;
  private VideoStreamLocatorPlaylist deletablePlaylist;

  @Value("${DATA_ROOT}/videos")
  private String baseVideoStorage;

  @Autowired
  public VideoStreamingServiceTest(
      FileServerPluginService fileServerPluginService,
      TestDataCreator testDataCreator,
      FileServerUserService userService,
      VideoStreamingService streamingService,
      VideoStreamLocatorPlaylistService playlistService,
      VideoStreamLocatorService streamLocatorService) {
    // static ref for cleanup
    VideoStreamingServiceTest.streamingService = streamingService;
    this.streamLocatorService = streamLocatorService;
    this.playlistService = playlistService;
    setup(fileServerPluginService, testDataCreator, userService);
  }

  @AfterAll
  static void cleanup() throws IOException {
    TestDataCreator.deleteGeneratedMatchArtwork(cleanupData);
    logger.info("Attempting to delete test data...");
    for (String location : videoStorageDirs) {
      deleteDataIn(location);
    }
    logger.info("Test data successfully deleted.");
  }

  private static void deleteDataIn(String location) throws IOException {
    try {
      logger.info("Deleting data in: {}", location);
      final Path path = Path.of(location);
      Files.walkFileTree(path, new RecursiveDirectoryDeleter());
      logger.info("Location: {} deleted? {}", location, !path.toFile().exists());
    } catch (FileNotFoundException | NoSuchFileException e) {
      logger.info("No directory found at: {}; skipping...", location);
    }
  }

  @NotNull
  private static VideoFileSource getTestFileSource(@NotNull Event testMatch) {
    AtomicReference<VideoFileSource> atomicFileSource = new AtomicReference<>();
    final Set<VideoFileSource> fileSources = testMatch.getFileSources();
    fileSources.stream().findFirst().ifPresent(atomicFileSource::set);
    final VideoFileSource testFileSource = atomicFileSource.get();

    assertThat(testFileSource).isNotNull();
    return testFileSource;
  }

  private void setup(
      @NotNull FileServerPluginService fileServerPluginService,
      @NotNull TestDataCreator testDataCreator,
      @NotNull FileServerUserService userService) {
    fileServerPluginService.enablePlugin(TestFileServerPlugin.PLUGIN_ID);

    // Create test user & login
    FileServerUser testFileServerUser = testDataCreator.createTestFileServerUser();
    userService.login(testFileServerUser);
    assertThat(testFileServerUser.isLoggedIn()).isTrue();

    // Create test data
    this.testMatch = testDataCreator.createTestMatch();
    VideoStreamingServiceTest.testFileSource = getTestFileSource(this.testMatch);
    cleanupData.add(testMatch);
    this.testVideoFile =
        testFileSource.getVideoFilePacks().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No VideoFilePacks for test data"))
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No VideoFiles for test data"));
  }

  @Test
  @Order(1)
  @DisplayName("Validate retrieval of file sources for a given Event")
  void fetchVideoFileSources() {
    logger.info("Fetching event with ID: {}", testMatch.getEventId());
    final Collection<VideoFileSource> actualFileSources = testMatch.getFileSources();
    logger.info("Retrieved files sources: {}", actualFileSources);

    final Set<VideoFileSource> expectedFileSources = testMatch.getFileSources();
    assertThat(actualFileSources.size()).isEqualTo(expectedFileSources.size());
    assertThat(actualFileSources).containsAll(expectedFileSources);
  }

  @Test
  @Order(2)
  @DisplayName("Test that a playlist is created & returned")
  @Transactional
  void getVideoStreamPlaylist() {
    // test variables
    final UUID testEventId = testMatch.getEventId();
    final UUID testFileSrcId = testFileSource.getFileSrcId();
    logger.info("Testing with Match:\n{}", testMatch);
    logger.info(
        "Testing video stream creation with Event ID: {}, File Source ID: {}",
        testEventId,
        testFileSrcId);

    final Optional<VideoPlaylist> playlistOptional =
        streamingService.beginStreamingVideo(testMatch, testFileSrcId);
    assertThat(playlistOptional).isPresent();
    videoStorageDirs.add(baseVideoStorage + "\\" + testFileSrcId); // for cleanup

    final VideoPlaylist videoPlaylist = playlistOptional.get();
    logger.info("Retrieved VideoPlaylist: {}", videoPlaylist);

    assertThat(videoPlaylist).isNotNull();
    assertThat(videoPlaylist.getLocatorIds().size()).isNotZero();

    final Optional<VideoPlaylist> afterCreatingStreamPlaylist =
        streamingService.beginStreamingVideo(testMatch, testFileSrcId);
    assertThat(afterCreatingStreamPlaylist).isNotNull().isPresent();
    if (afterCreatingStreamPlaylist.isPresent()) {
      final VideoPlaylist playlist = afterCreatingStreamPlaylist.get();
      assertThat(playlist).isNotNull();
      final int streamingTaskCount = playlist.getLocatorIds().size();
      assertThat(streamingTaskCount).isNotZero();
      logger.info("There will be: {} streaming tasks...", streamingTaskCount);
      this.testStreamTaskCount = streamingTaskCount;

      // for data deletion test
      Optional<VideoStreamLocatorPlaylist> deletableOptional =
          playlistService.getVideoStreamPlaylistFor(testFileSrcId);
      assertThat(deletableOptional).isPresent();
      deletablePlaylist = deletableOptional.get();
    }
  }

  @Test
  @Order(3)
  @DisplayName("Validate reading playlist file from disk")
  void readPlaylistFile() throws Exception {
    final int MIN_PLAYLIST_LEN = 100;

    logger.info("Waiting {} seconds to ensure stream has started...", WAIT_SECONDS);
    TimeUnit.SECONDS.sleep(WAIT_SECONDS);
    logger.info("Done waiting. Proceeding with test...");

    final VideoStreamLocator testStreamLocator = getTestStreamLocator();
    logger.info(
        "Testing playlist file reading with File Source ID: {}, Stream Locator ID: {}",
        testFileSource.getFileSrcId(),
        testStreamLocator.getStreamLocatorId());

    // Read test playlist file
    final String actualPlaylistData =
        streamingService.readPlaylistFile(testStreamLocator.getStreamLocatorId());
    final int actualPlaylistSize = actualPlaylistData.getBytes(StandardCharsets.UTF_8).length;
    logger.info("Read playlist data:\n{}", actualPlaylistData);
    logger.info("Data length: {}", actualPlaylistSize);
    logger.info("Ensuring read playlist is longer than {} bytes", MIN_PLAYLIST_LEN);
    assertThat(actualPlaylistSize).isGreaterThan(MIN_PLAYLIST_LEN);
  }

  @Test
  @Order(4)
  @DisplayName("Validate reading of video segment (.ts) from disk")
  void getVideoSegmentResource() throws IOException, InterruptedException {
    logger.info("Waiting {} seconds before proceeding with test...", WAIT_SECONDS);
    TimeUnit.SECONDS.sleep(WAIT_SECONDS);

    final long minContentLength = 500_000L;
    final VideoStreamLocator testStreamLocator = getTestStreamLocator();

    // Test params
    final Long testStreamLocatorId = testStreamLocator.getStreamLocatorId();
    final UUID testVideoFileSrcId = testFileSource.getFileSrcId();
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

    logger.debug("Read video segment: {}", actualVideoResource);
    assertThat(actualVideoResource).isNotNull();
    assertThat(actualVideoResource.contentLength()).isGreaterThan(minContentLength);
  }

  @NotNull
  private VideoStreamLocator getTestStreamLocator() {
    final Optional<VideoStreamLocator> locatorOptional =
        streamLocatorService.getStreamLocatorFor(testVideoFile.getFileId());
    assertThat(locatorOptional).isPresent();
    final VideoStreamLocator testStreamLocator = locatorOptional.get();
    assertThat(testStreamLocator).isNotNull();
    return testStreamLocator;
  }

  @Test
  @Order(5)
  @DisplayName("Ensure streaming service can kill all current streaming tasks")
  void killAllStreamingTasks() {
    final int initialTasks = streamingService.getActiveStreamingTaskCount();
    logger.info("Before killing, there are {} active streaming tasks...", initialTasks);

    logger.info(
        "Executing killAllStreamingTasks() {} times, in case streaming is scheduled sequentially",
        this.testStreamTaskCount);
    int actualTasksKilled = 0;
    for (int i = 0; i < this.testStreamTaskCount; i++) {
      logger.info("Attempting to kill all tasks...");
      actualTasksKilled += streamingService.killAllStreamingTasks();
    }

    logger.info("Service killed: {} tasks", actualTasksKilled);
    assertThat(actualTasksKilled).isGreaterThanOrEqualTo(initialTasks);

    final int tasksAfterKilling = streamingService.getActiveStreamingTaskCount();
    logger.info("After killing, there are: {} active streaming tasks...", tasksAfterKilling);
  }

  @Test
  @Order(6)
  @DisplayName("Validate ability to delete previously downloaded video data")
  void deleteVideoData() throws IOException, InterruptedException {
    // given
    assertThat(deletablePlaylist).isNotNull();
    final List<VideoStreamLocator> streamLocators = deletablePlaylist.getStreamLocators();

    // when
    streamingService.killAllStreamingTasks();
    logger.info("Waiting {} seconds for dust to settle...", WAIT_SECONDS);
    TimeUnit.SECONDS.sleep(WAIT_SECONDS);
    logger.info("Deleting video data...");
    streamingService.deleteAllVideoData(deletablePlaylist);

    // then
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
  @DisplayName("Validate ability to kill all streaming tasks")
  @Transactional
  public void killStreamingFor() throws InterruptedException {
    logger.info(
        "Before testing, there are: {} active streaming tasks...",
        streamingService.getActiveStreamingTaskCount());

    final VideoFileSource testFileSource = getTestFileSource(this.testMatch);
    final UUID fileSrcId = testFileSource.getFileSrcId();
    logger.info(
        "Beginning test stream for file source stream killing with file source ID: {}", fileSrcId);

    final Optional<VideoPlaylist> playlistOptional =
        streamingService.beginStreamingVideo(testMatch, fileSrcId);
    assertThat(playlistOptional).isPresent();
    final VideoPlaylist videoPlaylist = playlistOptional.get();
    logger.info("Using playlist: {}", videoPlaylist);
    final int streamTaskCount = videoPlaylist.getLocatorIds().size();
    logger.info("There are: {} streaming tasks for this test...", streamTaskCount);
    final int timeout = 5;
    logger.info("Waiting {} seconds for stream to start...", timeout);
    TimeUnit.SECONDS.sleep(timeout);

    logger.info(
        "Executing killAllStreamsFor()... {} times, in case streaming is scheduled sequentially...",
        streamTaskCount);
    int killCount = 0;
    for (int i = 0; i < streamTaskCount; i++) {
      killCount += streamingService.killAllStreamsFor(fileSrcId);
    }
    logger.info("Killed {} tasks in total", killCount);
    assertThat(killCount).isGreaterThanOrEqualTo(streamTaskCount);
  }

  @Test
  @DisplayName("Validate that sorting VideoStreamLocatorPlaylist locators works as expected")
  public void testPlaylistLocatorSorting() throws IOException {
    final VideoStreamLocatorPlaylist playlist = new VideoStreamLocatorPlaylist();
    final SingleStreamLocator firstHalfLocator = new SingleStreamLocator();
    final SingleStreamLocator secondHalfLocator = new SingleStreamLocator();
    final URL url = new URL("https://www.com.com");
    final VideoFile firstHalf = new VideoFile(PartIdentifier.FIRST_HALF, url);
    final VideoFile secondHalf = new VideoFile(PartIdentifier.SECOND_HALF, url);
    firstHalfLocator.setVideoFile(firstHalf);
    secondHalfLocator.setVideoFile(secondHalf);

    // add second half first
    playlist.addStreamLocator(secondHalfLocator);
    playlist.addStreamLocator(firstHalfLocator);

    final List<VideoStreamLocator> locators = playlist.getStreamLocators();
    logger.info("Locators before sorting: {}", locators);
    logger.info("Sorting...");

    locators.sort(Comparator.comparing(VideoStreamLocator::getVideoFile));
    logger.info("Locators AFTER sorting: {}", locators);

    final VideoStreamLocator firstLocator = locators.get(0);
    logger.info("First locator after sorting is: {}", firstLocator);
    assertThat(firstLocator).isEqualTo(firstHalfLocator);
  }
}
