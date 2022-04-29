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

package self.me.matchday.api.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.api.service.video.VideoFileService;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.model.video.PartIdentifier;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.plugin.fileserver.TestFileServerPlugin;
import self.me.matchday.plugin.io.ffmpeg.FFmpegMetadata;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for VideoFile refresh service")
class VideoFileServiceTest {

  private static final Logger logger = LogManager.getLogger(VideoFileServiceTest.class);

  // Test resources
  private static TestDataCreator testDataCreator;
  private static VideoFileService videoFileService;

  private static VideoFileSource testVideoFileSrc;
  private static FileServerUser testFileServerUser;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull final TestDataCreator testDataCreator,
      @Autowired @NotNull final VideoFileService videoFileService,
      @Autowired @NotNull final FileServerService fileServerService,
      @Autowired @NotNull final TestFileServerPlugin testFileServerPlugin) {

    VideoFileServiceTest.testDataCreator = testDataCreator;
    VideoFileServiceTest.videoFileService = videoFileService;

    // Create test user & login
    VideoFileServiceTest.testFileServerUser = testDataCreator.createTestFileServerUser();
    fileServerService.login(testFileServerUser, testFileServerPlugin.getPluginId());
    assertThat(testFileServerUser.isLoggedIn()).isTrue();

    // Create test VideoFileSource
    testVideoFileSrc = testDataCreator.createTestVideoFileSource();
  }

  @AfterAll
  static void tearDown() {
    // Remove test user from repo
    logger.info("Deleting test user: {}", testFileServerUser);
    testDataCreator.deleteFileServerUser(testFileServerUser);
    testDataCreator.deleteVideoFileSource(testVideoFileSrc);
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
}
