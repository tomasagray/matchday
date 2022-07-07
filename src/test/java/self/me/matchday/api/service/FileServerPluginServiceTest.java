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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.plugin.fileserver.FileServerPlugin;
import self.me.matchday.plugin.fileserver.TestFileServerPlugin;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for remote file server PLUGIN management service")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class FileServerPluginServiceTest {

  private static final Logger logger = LogManager.getLogger(FileServerPluginServiceTest.class);

  private static TestDataCreator testDataCreator;
  private static FileServerPluginService fileServerPluginService;
  private static FileServerUserService userService;
  private static FileServerPlugin testFileServerPlugin;
  private static FileServerUser testFileServerUser;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull final TestDataCreator testDataCreator,
      @Autowired final FileServerPluginService fileServerPluginService,
      @Autowired FileServerUserService userService,
      @Autowired final TestFileServerPlugin testFileServerPlugin) {

    FileServerPluginServiceTest.testDataCreator = testDataCreator;
    FileServerPluginServiceTest.fileServerPluginService = fileServerPluginService;
    FileServerPluginServiceTest.userService = userService;
    FileServerPluginServiceTest.testFileServerPlugin = testFileServerPlugin;

    testFileServerUser = testDataCreator.createTestFileServerUser();
  }

  @Test
  @DisplayName("Test retrieval of registered file server plugin by ID")
  void getPluginById() {

    logger.info("Attempting to get test plugin with ID: {}", testFileServerPlugin.getPluginId());
    final Optional<FileServerPlugin> pluginOptional =
        fileServerPluginService.getPluginById(testFileServerPlugin.getPluginId());
    assertThat(pluginOptional).isPresent();

    pluginOptional.ifPresent(
        fileServerPlugin -> {
          logger.info("Successfully found plugin: {}", fileServerPlugin);
          assertThat(fileServerPlugin.getPluginId()).isEqualTo(testFileServerPlugin.getPluginId());
          assertThat(fileServerPlugin.getTitle()).isEqualTo(testFileServerPlugin.getTitle());
          assertThat(fileServerPlugin.getDescription())
              .isEqualTo(testFileServerPlugin.getDescription());
        });
  }

  @Test
  @DisplayName("Validate retrieval of all file server plugins")
  void getFileServerPlugins() {

    final int expectedPluginCount = 2;
    final List<FileServerPlugin> fileServerPlugins = fileServerPluginService.getFileServerPlugins();
    final int actualPluginCount = fileServerPlugins.size();
    logger.info("Found FileServerPlugins:\n{}", fileServerPlugins);
    logger.info("Found: {} plugins; expected: {}", actualPluginCount, expectedPluginCount);

    assertThat(actualPluginCount).isGreaterThanOrEqualTo(expectedPluginCount);
    assertThat(fileServerPlugins).contains(testFileServerPlugin);
  }

  @Test
  @DisplayName("Test enabling & disabling of plugin")
  void testPluginEnableAndDisable() {

    final UUID testPluginId = testFileServerPlugin.getPluginId();

    // test default enable
    logger.info("Verifying plugin is enabled by default...");
    assertThat(fileServerPluginService.isPluginEnabled(testPluginId)).isTrue();

    // test disable
    logger.info("Verifying plugin can be disabled...");
    fileServerPluginService.disablePlugin(testPluginId);
    assertThat(fileServerPluginService.isPluginEnabled(testPluginId)).isFalse();

    // test re-enable
    logger.info("Verifying plugin can be re-enabled...");
    fileServerPluginService.enablePlugin(testPluginId);
    assertThat(fileServerPluginService.isPluginEnabled(testPluginId)).isTrue();
  }

  @Test
  @DisplayName("Validate plugin internal URL extraction")
  void getDownloadUrl() throws IOException {

    // Ensure user is logged in
    userService.login(testFileServerUser);

    final URL firstHalfUrl = testDataCreator.getFirstHalfUrl();
    assertThat(firstHalfUrl).isNotNull();

    final Optional<URL> optionalURL = fileServerPluginService.getDownloadUrl(firstHalfUrl);
    assertThat(optionalURL.isPresent()).isTrue();
    optionalURL.ifPresent(
        url -> {
          logger.info("Got download URL from plugin: " + url);
          assertThat(url).isNotNull();
        });
  }

  @Test
  @DisplayName("Validate plugin refresh rate retrieval in plugin service")
  void getFileServerRefreshRate() {

    final URL firstHalfUrl = testDataCreator.getFirstHalfUrl();
    assertThat(firstHalfUrl).isNotNull();
    logger.info("Testing server refresh rate for URL: {}", firstHalfUrl);

    final Duration actualServerRefreshRate =
        fileServerPluginService.getFileServerRefreshRate(firstHalfUrl);
    final Duration expectedServerRefreshRate = testFileServerPlugin.getRefreshRate();
    assertThat(actualServerRefreshRate).isEqualTo(expectedServerRefreshRate);
  }
}
