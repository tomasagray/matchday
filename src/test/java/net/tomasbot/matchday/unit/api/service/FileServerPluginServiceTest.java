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
import static net.tomasbot.matchday.config.settings.EnabledFileServerPlugins.ENABLED_FILESERVERS;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import net.tomasbot.matchday.TestDataCreator;
import net.tomasbot.matchday.TestFileServerPlugin;
import net.tomasbot.matchday.api.service.FileServerPluginService;
import net.tomasbot.matchday.api.service.FileServerUserService;
import net.tomasbot.matchday.api.service.PluginService;
import net.tomasbot.matchday.model.FileServerUser;
import net.tomasbot.matchday.plugin.fileserver.FileServerPlugin;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for remote file server PLUGIN management service")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class FileServerPluginServiceTest {

  private static final Logger logger = LogManager.getLogger(FileServerPluginServiceTest.class);

  private final TestDataCreator testDataCreator;
  private final FileServerPluginService fileServerPluginService;
  private final FileServerUserService userService;
  private final FileServerPlugin testFileServerPlugin;
  private final FileServerUser testFileServerUser;

  @Autowired
  public FileServerPluginServiceTest(
      @NotNull PluginService pluginService,
      @NotNull TestDataCreator testDataCreator,
      FileServerPluginService fileServerPluginService,
      FileServerUserService userService,
      TestFileServerPlugin testFileServerPlugin) {
    this.testDataCreator = testDataCreator;
    this.fileServerPluginService = fileServerPluginService;
    this.userService = userService;
    this.testFileServerPlugin = testFileServerPlugin;

    // setup
    this.testFileServerUser = testDataCreator.createTestFileServerUser();
    pluginService.enablePlugin(testFileServerPlugin, ENABLED_FILESERVERS);
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
          logger.info("Got download URL from plugin: {}", url);
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
