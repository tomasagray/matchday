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

package net.tomasbot.matchday.unit.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;

import net.tomasbot.ffmpeg_wrapper.metadata.FFmpegMetadata;
import net.tomasbot.matchday.TestDataCreator;
import net.tomasbot.matchday.TestFileServerPlugin;
import net.tomasbot.matchday.api.service.FileServerPluginService;
import net.tomasbot.matchday.api.service.FileServerUserService;
import net.tomasbot.matchday.api.service.video.VideoFileService;
import net.tomasbot.matchday.model.FileServerUser;
import net.tomasbot.matchday.model.video.PartIdentifier;
import net.tomasbot.matchday.model.video.VideoFile;
import net.tomasbot.matchday.model.video.VideoFileSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for VideoFile refresh service")
class VideoFileServiceTest {

  private static final Logger logger = LogManager.getLogger(VideoFileServiceTest.class);

  private final VideoFileService videoFileService;
  private VideoFileSource testVideoFileSrc;

  @Autowired
  public VideoFileServiceTest(
      TestDataCreator testDataCreator,
      FileServerUserService userService,
      FileServerPluginService fileServerPluginService,
      VideoFileService videoFileService) {
    this.videoFileService = videoFileService;
    setup(testDataCreator, userService, fileServerPluginService);
  }

  private void setup(
      @NotNull TestDataCreator testDataCreator,
      @NotNull FileServerUserService userService,
      @NotNull FileServerPluginService fileServerPluginService) {
    // Create test user & login
    final FileServerUser testFileServerUser = testDataCreator.createTestFileServerUser();
    userService.login(testFileServerUser);
    assertThat(testFileServerUser.isLoggedIn()).isTrue();
    fileServerPluginService.enablePlugin(TestFileServerPlugin.PLUGIN_ID);
    // Create test VideoFileSource
    this.testVideoFileSrc = testDataCreator.createVideoFileSourceAndSave();
  }

  @Test
  @DisplayName("Refresh data for a test VideoFile")
  void refreshVideoFileData() throws Exception {
    final int expectedStreamCount = 2;

    // Get test VideoFile
    final VideoFile testVideoFile =
        testVideoFileSrc.getVideoFilePacks().get(0).get(PartIdentifier.FIRST_HALF);
    // Refresh VideoFile
    final VideoFile testRefreshedVideoFile = videoFileService.refreshVideoFile(testVideoFile, true);

    // Perform tests
    logger.info(
        "Checking VideoFile: {}, internal URL: {}", testVideoFile, testVideoFile.getInternalUrl());

    final FFmpegMetadata metadata = testRefreshedVideoFile.getMetadata();
    // Test internal URL set
    assertThat(testRefreshedVideoFile.getInternalUrl()).isNotNull();
    // Test metadata set
    assertThat(metadata).isNotNull();
    assertThat(metadata.getStreams().size()).isGreaterThanOrEqualTo(expectedStreamCount);
  }

  @Test
  @DisplayName("Validate VideoFile is removed from locked list on error")
  void testVideoFileIsRemovedFromLockedListOnError() throws Exception {
    // given
    final URL url = new URL("https://www.nothing-land.com/non-existent-video.mp4");
    final VideoFile nonRefreshable = new VideoFile(PartIdentifier.DEFAULT, url);
    logger.info("Refreshing non-refreshable VideoFile: {}", nonRefreshable);

    // when
    VideoFile refreshed = null;
    try {
      refreshed = videoFileService.refreshVideoFile(nonRefreshable, false);
    } catch (Exception e) {
      logger.error("Caught error when refreshing: {}", e.getMessage());
    } finally {
      logger.info("After refresh, VideoFile is: {}", refreshed);
    }

    // then
    List<VideoFile> lockedVideoFiles = videoFileService.getLockedVideoFiles();
    logger.info("Currently locked VideoFiles: {}", lockedVideoFiles);
    assertThat(lockedVideoFiles).doesNotContain(nonRefreshable);
    assertThat(lockedVideoFiles).doesNotContain(refreshed);
  }
}
