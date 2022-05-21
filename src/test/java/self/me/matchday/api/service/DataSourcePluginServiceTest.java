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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.plugin.datasource.DataSourcePlugin;
import self.me.matchday.plugin.datasource.TestDataSourcePlugin;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("DataSourcePlugin testing & validation")
public class DataSourcePluginServiceTest {

  private static final Logger logger = LogManager.getLogger(DataSourcePluginServiceTest.class);
  private static DataSourcePluginService pluginService;
  private static DataSourcePlugin testDataSourcePlugin;

  @BeforeAll
  static void setup(
      @Autowired DataSourcePluginService pluginService,
      @Autowired TestDataSourcePlugin testDataSourcePlugin) {
    DataSourcePluginServiceTest.pluginService = pluginService;
    DataSourcePluginServiceTest.testDataSourcePlugin = testDataSourcePlugin;
  }

  @AfterAll
  static void tearDown() {
    logger.info("Deleting test data...");
    final boolean removed = pluginService.getDataSourcePlugins().remove(testDataSourcePlugin);
    assertThat(removed).isTrue();
  }

  @Test
  @DisplayName("Validate retrieval of a specific data source plugin by ID")
  void getDataSourcePlugin() {

    logger.info("Attempting to retrieve test plugin: {}", testDataSourcePlugin.getPluginId());
    // Retrieve test plugin
    final Optional<DataSourcePlugin> pluginOptional =
        pluginService.getDataSourcePlugin(testDataSourcePlugin.getPluginId());

    assertThat(pluginOptional).isPresent();
    pluginOptional.ifPresent(
        plugin -> logger.info("Successfully retrieved plugin: {}", plugin.getTitle()));
  }

  @Test
  @DisplayName("Ensure plugins can be enabled")
  void enablePlugin() {

    final UUID testPluginId = testDataSourcePlugin.getPluginId();

    // Attempt to enable plugin
    pluginService.enablePlugin(testPluginId);
    final boolean enabled =
        pluginService
            .getDataSourcePlugin(testPluginId)
            .map(DataSourcePlugin::isEnabled)
            .orElse(false);
    assertThat(enabled).isTrue();
    // Ensure plugin has been added to enabled plugins
    final Collection<DataSourcePlugin> enabledPlugins = pluginService.getEnabledPlugins();
    final Optional<DataSourcePlugin> pluginOptional =
        enabledPlugins.stream()
            .filter(plugin -> testPluginId.equals(plugin.getPluginId()))
            .findFirst();

    assertThat(pluginOptional).isPresent();
    pluginOptional.ifPresent(
        plugin -> logger.info("Plugin: {} successfully enabled", plugin.getTitle()));
  }

  @Test
  @DisplayName("Ensure a plugin can be disabled")
  void disablePlugin() {

    final UUID testPluginId = testDataSourcePlugin.getPluginId();
    logger.info("Attempting to disable plugin: {}", testPluginId);

    // Attempt to disable plugin
    pluginService.disablePlugin(testPluginId);
    final boolean enabled =
        pluginService
            .getDataSourcePlugin(testPluginId)
            .map(DataSourcePlugin::isEnabled)
            .orElse(true);
    assertThat(enabled).isFalse();
    logger.info("Successfully disabled plugin.");

    // Ensure plugin is NOT enabled
    final Collection<DataSourcePlugin> enabledPlugins = pluginService.getEnabledPlugins();
    final Optional<DataSourcePlugin> pluginOptional =
        enabledPlugins.stream()
            .filter(plugin -> testPluginId.equals(plugin.getPluginId()))
            .findAny();

    assertThat(pluginOptional).isNotPresent();

    logger.info("Re-enabling test DataSourcePlugin: " + testPluginId);
    pluginService.enablePlugin(testPluginId);
    final boolean nowEnabled =
        pluginService
            .getDataSourcePlugin(testPluginId)
            .map(DataSourcePlugin::isEnabled)
            .orElse(false);
    assertThat(nowEnabled).isTrue();
  }

  @Test
  @DisplayName("Validate plugin is correctly assessed as enabled")
  void isPluginEnabled() {

    final UUID testPluginId = testDataSourcePlugin.getPluginId();
    logger.info("Ensuring DataSourcePlugin: {} is enabled...", testPluginId);
    pluginService.enablePlugin(testPluginId);
    logger.info("Checking if test plugin: {} is enabled", testPluginId);

    final boolean pluginEnabled = pluginService.isPluginEnabled(testPluginId);
    assertThat(pluginEnabled).isTrue();
    logger.info("Plugin IS enabled");
  }

  @Test
  @DisplayName("Validate ALL data source plugins are returned by service")
  void getDataSourcePlugins() {

    final int expectedPluginCount = 3;

    // Retrieve all data source plugins
    final Collection<DataSourcePlugin> dataSourcePlugins = pluginService.getDataSourcePlugins();
    final int actualPluginCount = dataSourcePlugins.size();
    logger.info("Found: {} plugins", actualPluginCount);
    assertThat(actualPluginCount).isGreaterThanOrEqualTo(expectedPluginCount);
  }

  @Test
  @DisplayName("Validate correct # of ENABLED plugins is returned by service")
  void getEnabledPlugins() {

    final int expectedPluginCount = 2;

    final Collection<DataSourcePlugin> enabledPlugins = pluginService.getEnabledPlugins();
    final int actualPluginCount = enabledPlugins.size();
    logger.info("Found: {} enabled plugins", actualPluginCount);
    assertThat(actualPluginCount).isGreaterThanOrEqualTo(expectedPluginCount);
  }
}
