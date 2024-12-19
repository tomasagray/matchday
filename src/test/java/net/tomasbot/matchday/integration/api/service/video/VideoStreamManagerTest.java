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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.tomasbot.matchday.TestDataCreator;
import net.tomasbot.matchday.TestFileServerPlugin;
import net.tomasbot.matchday.api.service.FileServerPluginService;
import net.tomasbot.matchday.api.service.FileServerUserService;
import net.tomasbot.matchday.api.service.video.VideoStreamManager;
import net.tomasbot.matchday.model.FileServerUser;
import net.tomasbot.matchday.model.video.StreamJobState.JobStatus;
import net.tomasbot.matchday.model.video.TaskListState;
import net.tomasbot.matchday.model.video.VideoFileSource;
import net.tomasbot.matchday.model.video.VideoStreamLocator;
import net.tomasbot.matchday.model.video.VideoStreamLocatorPlaylist;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for video stream manager")
@Transactional
class VideoStreamManagerTest {

  private static final Logger logger = LogManager.getLogger(VideoStreamManagerTest.class);

  private static final int WAIT_SECONDS = 5;

  private final VideoStreamManager streamManager;
  private final ThreadPoolTaskExecutor executor;
  private final VideoFileSource testFileSource;
  private final FileServerPluginService dataSourcePluginService;

  @Autowired
  public VideoStreamManagerTest(
      @NotNull TestDataCreator testDataCreator,
      @NotNull FileServerUserService userService,
      @NotNull List<ThreadPoolTaskExecutor> executors,
      @NotNull FileServerPluginService dataSourcePluginService,
      VideoStreamManager streamManager) {
    this.streamManager = streamManager;
    this.testFileSource = testDataCreator.createVideoFileSourceAndSave();
    this.executor = executors.get(0);
    this.dataSourcePluginService = dataSourcePluginService;
    setup(testDataCreator, userService);
  }

  private static void setup(
      @NotNull TestDataCreator testDataCreator, @NotNull FileServerUserService userService) {
    final FileServerUser testFileServerUser = testDataCreator.createTestFileServerUser();
    logger.info("Logging in test user: {}", testFileServerUser);
    final FileServerUser loggedInUser = userService.login(testFileServerUser);

    final boolean loginStatus = loggedInUser.isLoggedIn();
    assertThat(loginStatus).isTrue();
  }

  private VideoStreamLocatorPlaylist createTestPlaylist() {
    VideoStreamLocatorPlaylist playlist = streamManager.createVideoStreamFrom(testFileSource);
    logger.info("Created test playlist: {}", playlist);
    return playlist;
  }

  private void enableFileServer() {
    UUID pluginId = TestFileServerPlugin.PLUGIN_ID;
    logger.info("Enabling TestFileServerPlugin {} ...", pluginId);
    dataSourcePluginService.enablePlugin(pluginId);
  }

  private VideoStreamLocator startVideoStream() {
    enableFileServer();
    VideoStreamLocatorPlaylist testPlaylist = createTestPlaylist();
    VideoStreamLocator locator = testPlaylist.getStreamLocators().get(0);
    assertThat(locator).isNotNull();
    streamManager.queueStreamJob(locator);
    return locator;
  }

  private void cleanup() throws IOException, InterruptedException {
    streamManager.killAllStreams();
    TimeUnit.SECONDS.sleep(WAIT_SECONDS);
    VideoStreamLocatorPlaylist playlist = getStreamLocatorPlaylist();
    streamManager.deleteLocalStreams(playlist);
  }

  @Test
  @DisplayName("Validate creation of VideoStreamLocatorPlaylists from VideoFileSources")
  void createVideoStreamFrom() {
    // given
    final JobStatus expectedStateStatus = JobStatus.CREATED;
    final double expectedCompletionRatio = 0.0;
    final int expectedStreamLocatorCount = 4;

    logger.info(
        "Testing VideoStreamLocatorPlaylist creation using File Source:\n{}", testFileSource);

    // get test data
    final VideoStreamLocatorPlaylist actualLocatorPlaylist =
        streamManager.createVideoStreamFrom(this.testFileSource);
    assertThat(actualLocatorPlaylist).isNotNull();
    final TaskListState actualState = actualLocatorPlaylist.getState();
    final JobStatus actualStateStatus = actualState.getStatus();
    final Double actualCompletionRatio = actualState.getCompletionRatio();
    final List<VideoStreamLocator> actualStreamLocators = actualLocatorPlaylist.getStreamLocators();

    logger.info(
        "VideoStreamManager created VideoStreamLocatorPlaylist:\n{}", actualLocatorPlaylist);

    assertThat(actualStateStatus).isEqualTo(expectedStateStatus);
    assertThat(actualCompletionRatio).isEqualTo(expectedCompletionRatio);
    assertThat(actualStreamLocators.size()).isEqualTo(expectedStreamLocatorCount);
  }

  @Test
  @DisplayName("Validate retrieval of previously created playlist")
  void getLocalStreamFor() {
    // given
    final UUID testFileSrcId = testFileSource.getFileSrcId();
    createTestPlaylist();
    logger.info("Attempting playlist lookup for file source ID: {}", testFileSrcId);

    // when
    final Optional<VideoStreamLocatorPlaylist> playlistOptional =
        streamManager.getLocalStreamFor(testFileSrcId);

    // then
    assertThat(playlistOptional).isPresent();
    logger.info("Successfully retrieved locator playlist: {}", playlistOptional.get());
  }

  @Test
  @DisplayName("Validate asynchronous video streaming")
  void queueStreamJob() throws InterruptedException, IOException {
    // given
    VideoStreamLocator locator = startVideoStream();

    // when
    logger.info("Beginning streaming of locator: {}", locator);
    streamManager.queueStreamJob(locator);

    logger.info("Giving stream a {}-second head start", WAIT_SECONDS);
    TimeUnit.SECONDS.sleep(WAIT_SECONDS);
    final boolean terminated =
        executor.getThreadPoolExecutor().awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS);
    logger.info("Wait terminated successfully? {}", terminated);

    // then
    final JobStatus actualStatus = locator.getState().getStatus();
    logger.info("Locator status after starting stream: {}", actualStatus);
    assertThat(actualStatus).isGreaterThanOrEqualTo(JobStatus.STARTED);

    cleanup();
  }

  @Test
  @DisplayName("Validate that streaming has registered in the database")
  void isStreamReady() throws InterruptedException, IOException {
    // given
    logger.info("There are currently: {} active streams", streamManager.getActiveStreamCount());
    VideoStreamLocator locator = startVideoStream();

    // when
    TimeUnit.SECONDS.sleep(WAIT_SECONDS);
    boolean terminated =
        executor.getThreadPoolExecutor().awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS);
    logger.info(
        "Testing status of Stream for VideoStreamLocator: {}", locator.getStreamLocatorId());
    logger.info("Done waiting; successful? {}; checking stream status...", terminated);

    // then
    streamManager
        .getLocalStreamFor(testFileSource.getFileSrcId())
        .ifPresent(
            off -> {
              List<VideoStreamLocator> streamLocators = off.getStreamLocators();
              streamLocators.forEach(System.out::println);
            });
    final JobStatus actualStreamStatus = locator.getState().getStatus();
    logger.info("Stream status was: {}", actualStreamStatus);
    assertThat(actualStreamStatus).isGreaterThanOrEqualTo(JobStatus.STREAMING);

    cleanup();
  }

  @Test
  @DisplayName("Validate VideoStreamManager can interrupt streaming tasks")
  void killAllStreamsFor() throws InterruptedException, IOException {
    // given
    startVideoStream();

    // when
    final VideoStreamLocatorPlaylist playlist = getStreamLocatorPlaylist();
    logger.info("Attempting to kill all streams for VideoStreamLocatorPlaylist: {}", playlist);
    streamManager.killAllStreamsFor(playlist);

    logger.info("Waiting {} seconds for streaming tasks to die...", WAIT_SECONDS);
    TimeUnit.SECONDS.sleep(WAIT_SECONDS);

    // then
    logger.info("Ensuring all tasks are dead");
    final VideoStreamLocatorPlaylist deadPlaylist = getStreamLocatorPlaylist();
    final JobStatus killedStatus = deadPlaylist.getState().getStatus();
    final boolean streamReady =
        killedStatus == JobStatus.COMPLETED || killedStatus == JobStatus.STREAMING;
    logger.info("JobStatus: {}", killedStatus);

    deadPlaylist
        .getStreamLocators()
        .forEach(
            stream -> {
              JobStatus status = stream.getState().getStatus();
              logger.info("Stream Locator: {}, status: {}", stream.getStreamLocatorId(), status);
            });
    assertThat(streamReady).isFalse();
    assertThat(killedStatus).isEqualTo(JobStatus.STOPPED);

    cleanup();
  }

  @Test
  @DisplayName("Ensure VideoStreamManager can delete local data")
  void deleteLocalStream() throws IOException, InterruptedException {
    // given
    logger.info("Starting streaming for test...");
    startVideoStream();
    TimeUnit.SECONDS.sleep(10);

    logger.info("Killing test streams...");
    streamManager.killAllStreams();

    final UUID fileSrcId = testFileSource.getFileSrcId();
    final VideoStreamLocatorPlaylist playlist = getStreamLocatorPlaylist();
    final Path storageLocation = playlist.getStorageLocation();
    logger.info("Deleting local data associated with VideoStreamLocatorPlaylist: {}", playlist);

    // when
    streamManager.deleteLocalStreams(playlist);

    // then
    logger.info("Ensuring local data is actually gone...");
    assertThat(storageLocation).doesNotExist();
    final Optional<VideoStreamLocatorPlaylist> optionalAfterDelete =
        streamManager.getLocalStreamFor(fileSrcId);
    assertThat(optionalAfterDelete).isEmpty();
  }

  /**
   * Get a consistent playlist
   *
   * @return a VideoStreamLocatorPlaylist
   */
  private @NotNull VideoStreamLocatorPlaylist getStreamLocatorPlaylist() {
    final UUID fileSrcId = testFileSource.getFileSrcId();
    final Optional<VideoStreamLocatorPlaylist> playlistOptional =
        streamManager.getLocalStreamFor(fileSrcId);
    assertThat(playlistOptional).isPresent();
    return playlistOptional.get();
  }
}
