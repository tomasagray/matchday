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

package self.me.matchday.integration.api.service.video;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.api.service.FileServerUserService;
import self.me.matchday.api.service.video.VideoStreamLocatorPlaylistService;
import self.me.matchday.api.service.video.VideoStreamLocatorService;
import self.me.matchday.api.service.video.VideoStreamingService;
import self.me.matchday.model.Event;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.model.video.PartIdentifier;
import self.me.matchday.model.video.SingleStreamLocator;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoPlaylist;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for video streaming service")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(Lifecycle.PER_CLASS)
class VideoStreamingServiceTest {

  private static final Logger logger = LogManager.getLogger(VideoStreamingServiceTest.class);

  // Service dependencies
  private static VideoStreamingService streamingService;
  private final VideoStreamLocatorPlaylistService locatorPlaylistService;
  private final VideoStreamLocatorService streamLocatorService;

  // Test data
  private static final List<Event> cleanupData = new ArrayList<>();
  private final Event testMatch;
  private final VideoFile testVideoFile;
  private static VideoFileSource testFileSource;
  private int testStreamTaskCount;

  @Autowired
  public VideoStreamingServiceTest(
      @NotNull TestDataCreator testDataCreator,
      @NotNull FileServerUserService userService,
      VideoStreamingService streamingService,
      VideoStreamLocatorPlaylistService locatorPlaylistService,
      VideoStreamLocatorService streamLocatorService) {

    // static ref for cleanup
    VideoStreamingServiceTest.streamingService = streamingService;
    this.streamLocatorService = streamLocatorService;
    this.locatorPlaylistService = locatorPlaylistService;

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

  @AfterAll
  static void cleanup() throws IOException, InterruptedException {
    TestDataCreator.deleteGeneratedMatchArtwork(cleanupData);
    final int waitSec = 30;
    logger.info("Waiting {} seconds for dust to settle...", waitSec);
    TimeUnit.SECONDS.sleep(waitSec);
    logger.info("Dust must have settled. Proceeding with test...");

    logger.info("Attempting to delete test data...");
    streamingService.deleteAllVideoData(testFileSource.getFileSrcId());
    logger.info("Test data successfully deleted.");
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

  @Test
  @Order(1)
  @DisplayName("Validate retrieval of file sources for a given Event")
  void fetchVideoFileSources() {

    logger.info("Fetching event with ID: " + testMatch.getEventId());
    final Collection<VideoFileSource> actualFileSources = testMatch.getFileSources();
    logger.info("Retrieved files sources: " + actualFileSources);

    final Set<VideoFileSource> expectedFileSources = testMatch.getFileSources();
    assertThat(actualFileSources.size()).isEqualTo(expectedFileSources.size());
    assertThat(actualFileSources).containsAll(expectedFileSources);
  }

  @Test
  @Order(2)
  @DisplayName("Test that a playlist is created & returned")
  void getVideoStreamPlaylist() {

    // test variables
    final UUID testEventId = testMatch.getEventId();
    final UUID testFileSrcId = testFileSource.getFileSrcId();
    logger.info("Testing with Match:\n" + testMatch);
    logger.info(
        "Testing video stream creation with Event ID: {}, File Source ID: {}",
        testEventId,
        testFileSrcId);

    final Optional<VideoPlaylist> playlistOptional =
        streamingService.getOrCreateVideoStreamPlaylist(testMatch, testFileSrcId);
    assertThat(playlistOptional).isPresent();
    final VideoPlaylist videoPlaylist = playlistOptional.get();
    logger.info("Retrieved VideoPlaylist: " + videoPlaylist);

    assertThat(videoPlaylist).isNotNull();
    assertThat(videoPlaylist.getLocatorIds().size()).isNotZero();

    final Optional<VideoPlaylist> afterCreatingStreamPlaylist =
        streamingService.getOrCreateVideoStreamPlaylist(testMatch, testFileSrcId);
    assertThat(afterCreatingStreamPlaylist).isNotNull().isPresent();
    final VideoPlaylist playlist = afterCreatingStreamPlaylist.get();
    assertThat(playlist).isNotNull();
    final int streamingTaskCount = playlist.getLocatorIds().size();
    assertThat(streamingTaskCount).isNotZero();
    logger.info("There will be: {} streaming tasks...", streamingTaskCount);
    this.testStreamTaskCount = streamingTaskCount;
  }

  @Test
  @Order(3)
  @DisplayName("Validate reading playlist file from disk")
  void readPlaylistFile() throws InterruptedException {

    final int MIN_PLAYLIST_LEN = 100;

    final int waitSeconds = 15;
    logger.info("Waiting {} seconds to ensure stream has started...", waitSeconds);
    TimeUnit.SECONDS.sleep(waitSeconds);
    logger.info("Done waiting. Proceeding with test...");

    final VideoStreamLocator testStreamLocator = getTestStreamLocator();
    logger.info(
        "Testing playlist file reading with File Source ID: {}, Stream Locator ID: {}",
        testFileSource.getFileSrcId(),
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

    logger.debug("Read video segment: " + actualVideoResource);
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
  void deleteVideoData() throws IOException {

    logger.info("Getting VideoStreamLocatorPlaylist for VideoFileSource: {}", testFileSource);
    final Optional<VideoStreamLocatorPlaylist> playlistOptional =
        locatorPlaylistService.getVideoStreamPlaylistFor(testFileSource.getFileSrcId());
    assertThat(playlistOptional).isPresent();
    VideoStreamLocatorPlaylist deletablePlaylist = playlistOptional.get();
    assertThat(deletablePlaylist).isNotNull();
    final List<VideoStreamLocator> streamLocators = deletablePlaylist.getStreamLocators();
    streamingService.killAllStreamingTasks();
    logger.info("Deleting video data...");
    streamingService.deleteAllVideoData(deletablePlaylist);

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
  @DisplayName("Validate ability to kill all streaming tasks")
  public void killStreamingFor() throws InterruptedException {

    logger.info(
        "Before testing, there are: {} active streaming tasks...",
        streamingService.getActiveStreamingTaskCount());

    final VideoFileSource testFileSource = getTestFileSource(this.testMatch);
    final UUID fileSrcId = testFileSource.getFileSrcId();
    logger.info(
        "Beginning test stream for file source stream killing with file source ID: " + fileSrcId);

    final Optional<VideoPlaylist> playlistOptional =
        streamingService.getOrCreateVideoStreamPlaylist(testMatch, fileSrcId);
    assertThat(playlistOptional).isPresent();
    final VideoPlaylist videoPlaylist = playlistOptional.get();
    logger.info("Using playlist: " + videoPlaylist);
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
