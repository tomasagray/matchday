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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.TestDataCreator;
import self.me.matchday.api.service.FileServerService;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.model.video.StreamJobState.JobStatus;
import self.me.matchday.model.video.TaskListState;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;
import self.me.matchday.plugin.fileserver.TestFileServerPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for video stream manager")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VideoStreamManagerTest {

  private static final Logger logger = LogManager.getLogger(VideoStreamManagerTest.class);
  private static VideoStreamManager streamManager;

  // test resources
  private static VideoFileSource testFileSource;
  private static VideoStreamLocator testStreamLocator;

  @BeforeAll
  public static void setup(
      @Autowired @NotNull final TestDataCreator testDataCreator,
      @Autowired @NotNull final FileServerService fileServerService,
      @Autowired @NotNull final TestFileServerPlugin testFileServerPlugin,
      @Autowired final VideoStreamManager streamManager) {

    VideoStreamManagerTest.streamManager = streamManager;
    VideoStreamManagerTest.testFileSource = testDataCreator.createTestVideoFileSource();

    final FileServerUser testFileServerUser = testDataCreator.createTestFileServerUser();
    final ClientResponse loginResponse =
        fileServerService.login(testFileServerUser, testFileServerPlugin.getPluginId());
    final HttpStatus loginStatus = loginResponse.statusCode();
    assertThat(loginStatus).isEqualTo(HttpStatus.OK);
  }

  @Test
  @Order(1)
  @DisplayName("Validate creation of VideoStreamLocatorPlaylists from VideoFileSources")
  void createVideoStreamFrom() {

    final JobStatus expectedStateStatus = JobStatus.CREATED;
    final double expectedCompletionRatio = 0.0;
    final int expectedStreamLocatorCount = 4;

    logger.info(
        "Testing VideoStreamLocatorPlaylist creation using File Source:\n{}", testFileSource);

    // get test data
    final VideoStreamLocatorPlaylist actualLocatorPlaylist =
        streamManager.createVideoStreamFrom(VideoStreamManagerTest.testFileSource);
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
  @Order(2)
  @DisplayName("Validate retrieval of previously created playlist")
  void getLocalStreamFor() {

    final UUID testFileSrcId = VideoStreamManagerTest.testFileSource.getFileSrcId();
    logger.info(
        "Attempting VideoStreamLocatorPlaylist lookup for file source ID: {}", testFileSrcId);

    final Optional<VideoStreamLocatorPlaylist> playlistOptional =
        streamManager.getLocalStreamFor(testFileSrcId);
    assertThat(playlistOptional).isPresent();
    logger.info("Successfully retrieved locator playlist: {}", playlistOptional.get());
  }

  @Test
  @Order(3)
  @DisplayName("Validate asynchronous video streaming")
  void beginStreaming() throws InterruptedException {

    final int streamHeadStartSeconds = 10;

    final VideoStreamLocatorPlaylist testPlaylist = getStreamLocatorPlaylist();
    VideoStreamManagerTest.testStreamLocator = testPlaylist.getStreamLocators().get(0);
    assertThat(testStreamLocator).isNotNull();

    logger.info("Beginning streaming of locator: {}", testStreamLocator);
    streamManager.beginStreaming(testStreamLocator);
    logger.info(String.format("Giving stream a %d-second head start", streamHeadStartSeconds));
    TimeUnit.SECONDS.sleep(streamHeadStartSeconds);

    final JobStatus actualStatus = testStreamLocator.getState().getStatus();
    logger.info("Locator status after starting stream: {}", actualStatus);
    assertThat(actualStatus).isGreaterThanOrEqualTo(JobStatus.STARTED);
  }

  @Test
  @Order(4)
  @DisplayName("Validate that streaming has registered in the database")
  void isStreamReady() throws InterruptedException {

    final long waitSeconds = 30;

    logger.info(
        "Testing status of Stream for VideoStreamLocator: "
            + testStreamLocator.getStreamLocatorId());
    logger.info(String.format("Waiting %d seconds before checking stream...", waitSeconds));
    TimeUnit.SECONDS.sleep(waitSeconds);
    logger.info("Done waiting, checking stream status...");

    final JobStatus actualStreamStatus = testStreamLocator.getState().getStatus();
    logger.info("Stream status was: {}", actualStreamStatus);
    assertThat(actualStreamStatus).isGreaterThanOrEqualTo(JobStatus.STREAMING);
  }

  @Test
  @Order(5)
  @DisplayName("Validate VideoStreamManager can interrupt streaming tasks")
  @Transactional
  void killAllStreamsFor() throws InterruptedException {

    final int waitToDie = 10;

    final VideoStreamLocatorPlaylist playlist = getStreamLocatorPlaylist();
    logger.info("Attempting to kill all streams for VideoStreamLocatorPlaylist: {}", playlist);
    streamManager.killAllStreamsFor(playlist);

    logger.info(String.format("Waiting %d seconds for streaming tasks to die...", waitToDie));
    TimeUnit.SECONDS.sleep(waitToDie);

    logger.info("Ensuring all tasks are dead");
    final VideoStreamLocatorPlaylist deadPlaylist = getStreamLocatorPlaylist();
    final JobStatus killedStatus = deadPlaylist.getState().getStatus();
    final boolean streamReady =
        killedStatus == JobStatus.COMPLETED || killedStatus == JobStatus.STREAMING;
    logger.info("JobStatus: {}", killedStatus);
    deadPlaylist
        .getStreamLocators()
        .forEach(
            locator ->
                logger.info(
                    "Stream Locator: {}, status: {}",
                    locator.getStreamLocatorId(),
                    locator.getState().getStatus()));
    assertThat(streamReady).isFalse();
    assertThat(killedStatus).isEqualTo(JobStatus.STOPPED);
  }

  @Test
  @Order(6)
  @DisplayName("Ensure VideoStreamManager can delete local data")
  void deleteLocalStream() throws IOException {

    final UUID fileSrcId = testFileSource.getFileSrcId();
    final VideoStreamLocatorPlaylist playlist = getStreamLocatorPlaylist();
    final Path storageLocation = playlist.getStorageLocation();
    logger.info("Deleting local data associated with VideoStreamLocatorPlaylist: {}", playlist);

    streamManager.deleteLocalStream(playlist);

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
