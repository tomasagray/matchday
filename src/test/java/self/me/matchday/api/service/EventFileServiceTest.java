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

package self.me.matchday.api.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.CreateTestData;
import self.me.matchday.TestFileServerPlugin;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.plugin.io.ffmpeg.FFmpegMetadata;
import self.me.matchday.util.Log;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for EventFile refresh service")
class EventFileServiceTest {

  private static final String LOG_TAG = "EventFileServiceTest";

  // Test resources
  private static EventFileService eventFileService;
  private static FileServerService fileServerService;

  private static EventFileSource testEventFileSrc;
  private static FileServerUser testFileServerUser;

  @BeforeAll
  static void setUp(
      @Autowired final EventFileService eventFileService,
      @Autowired final FileServerService fileServerService,
      @Autowired final TestFileServerPlugin testFileServerPlugin) {

    EventFileServiceTest.eventFileService = eventFileService;
    EventFileServiceTest.fileServerService = fileServerService;

    // Create & register test file server plugin
    // Create test user & login
    EventFileServiceTest.testFileServerUser = CreateTestData.createTestFileServerUser();
    fileServerService.login(testFileServerUser, testFileServerPlugin.getPluginId());
    assertThat(testFileServerUser.isLoggedIn()).isTrue();

    // Create test EventFileSource
    testEventFileSrc = CreateTestData.createTestEventFileSource();
  }

  @AfterAll
  static void tearDown() {

    // Remove test user from repo
    Log.i(LOG_TAG, "Deleting test user: " + testFileServerUser);
    fileServerService.deleteUser(testFileServerUser.getUserId());
  }

  @Test
  @DisplayName("Refresh data for a test EventFile")
  void refreshEventFileData() throws ExecutionException, InterruptedException {

    final int expectedStreamCount = 2;

    // Get test EventFile
    final EventFile testEventFile = testEventFileSrc.getEventFiles().get(0);
    // Refresh EventFile
    final EventFile testRefreshedEventFile = eventFileService.refreshEventFile(testEventFile, true);

    // Perform tests
    Log.i(
        LOG_TAG,
        String.format(
            "Checking EventFile: %s, internal URL: %s",
            testEventFile, testEventFile.getInternalUrl()));

    final FFmpegMetadata metadata = testRefreshedEventFile.getMetadata();
    // Test internal URL set
    assertThat(testRefreshedEventFile.getInternalUrl()).isNotNull();
    // Test metadata set
    assertThat(metadata).isNotNull();
    assertThat(metadata.getStreams().size()).isGreaterThanOrEqualTo(expectedStreamCount);
  }
}
