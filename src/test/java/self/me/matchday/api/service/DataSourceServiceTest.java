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
import self.me.matchday.model.*;
import self.me.matchday.plugin.datasource.DataSourcePlugin;
import self.me.matchday.plugin.datasource.TestDataSourcePlugin;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for all data source refresh service")
class DataSourceServiceTest {

  private static final String LOG_TAG = "DataSourceServiceTest";

  private static DataSourceService dataSourceService;
  private static TestDataCreator testDataCreator;
  private static EventService eventService;
  private static DataSourcePlugin testDataSourcePlugin;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull DataSourceService dataSourceService,
      @Autowired @NotNull TestDataCreator testDataCreator,
      @Autowired EventService eventService,
      @Autowired TestDataSourcePlugin testDataSourcePlugin) {

    DataSourceServiceTest.dataSourceService = dataSourceService;
    DataSourceServiceTest.testDataCreator = testDataCreator;
    DataSourceServiceTest.eventService = eventService;

    DataSourceServiceTest.testDataSourcePlugin = testDataSourcePlugin;
    //    dataSourceService.getDataSourcePlugins().add(testDataSourcePlugin);
  }

  @AfterAll
  static void tearDown() {

    Log.i(LOG_TAG, "Deleting test data...");
    final boolean removed = dataSourceService.getDataSourcePlugins().remove(testDataSourcePlugin);
    assertThat(removed).isTrue();
  }

  @Test
  @DisplayName("Refresh all data sources")
  void refreshAllDataSources() throws IOException {

    final int expectedEventCount = 1;

    final SnapshotRequest testRequest = SnapshotRequest.builder().build();
    Log.i(LOG_TAG, "Testing Data Source Service refresh with Snapshot Request:\n" + testRequest);

    final SnapshotRequest testResult = dataSourceService.refreshAllDataSources(testRequest);
    assertThat(testResult).isEqualTo(testRequest);

    // Ensure some data was collected by request
    final List<Event> events = eventService.fetchAllEvents();
    final int actualEventCount = events.size();
    assertThat(actualEventCount).isGreaterThanOrEqualTo(expectedEventCount);
  }

  @Test
  @DisplayName("Validate retrieval of a specific data source plugin by ID")
  void getDataSourcePlugin() {

    Log.i(LOG_TAG, "Attempting to retrieve test plugin: " + testDataSourcePlugin.getPluginId());
    // Retrieve test plugin
    final Optional<DataSourcePlugin> pluginOptional =
        dataSourceService.getDataSourcePlugin(
            DataSourceServiceTest.testDataSourcePlugin.getPluginId());

    assertThat(pluginOptional).isPresent();
    pluginOptional.ifPresent(
        plugin -> Log.i(LOG_TAG, "Successfully retrieved plugin: " + plugin.getTitle()));
  }

  @Test
  @DisplayName("Ensure plugins can be enabled")
  void enablePlugin() {

    final UUID testPluginId = testDataSourcePlugin.getPluginId();

    // Attempt to enable plugin
    dataSourceService.enablePlugin(testPluginId);
    final boolean enabled =
        dataSourceService
            .getDataSourcePlugin(testPluginId)
            .map(DataSourcePlugin::isEnabled)
            .orElse(false);
    assertThat(enabled).isTrue();
    // Ensure plugin has been added to enabled plugins
    final Collection<DataSourcePlugin> enabledPlugins = dataSourceService.getEnabledPlugins();
    final Optional<DataSourcePlugin> pluginOptional =
        enabledPlugins.stream()
            .filter(plugin -> testPluginId.equals(plugin.getPluginId()))
            .findFirst();

    assertThat(pluginOptional).isPresent();
    pluginOptional.ifPresent(
        plugin ->
            Log.i(LOG_TAG, String.format("Plugin: %s successfully enabled", plugin.getTitle())));
  }

  @Test
  @DisplayName("Ensure a plugin can be disabled")
  void disablePlugin() {

    final UUID testPluginId = testDataSourcePlugin.getPluginId();
    Log.i(LOG_TAG, "Attempting to disable plugin: " + testPluginId);

    // Attempt to disable plugin
    dataSourceService.disablePlugin(testPluginId);
    final boolean enabled =
        dataSourceService
            .getDataSourcePlugin(testPluginId)
            .map(DataSourcePlugin::isEnabled)
            .orElse(true);
    assertThat(enabled).isFalse();
    Log.i(LOG_TAG, "Successfully disabled plugin.");

    // Ensure plugin is NOT enabled
    final Collection<DataSourcePlugin> enabledPlugins = dataSourceService.getEnabledPlugins();
    final Optional<DataSourcePlugin> pluginOptional =
        enabledPlugins.stream()
            .filter(plugin -> testPluginId.equals(plugin.getPluginId()))
            .findAny();

    assertThat(pluginOptional).isNotPresent();

    Log.i(LOG_TAG, "Re-enabling test DataSourcePlugin: " + testPluginId);
    dataSourceService.enablePlugin(testPluginId);
    final boolean nowEnabled =
        dataSourceService
            .getDataSourcePlugin(testPluginId)
            .map(DataSourcePlugin::isEnabled)
            .orElse(false);
    assertThat(nowEnabled).isTrue();
  }

  @Test
  @DisplayName("Validate plugin is correctly assessed as enabled")
  void isPluginEnabled() {

    final UUID testPluginId = testDataSourcePlugin.getPluginId();
    Log.i(LOG_TAG, String.format("Ensuring DataSourcePlugin: %s is enabled...", testPluginId));
    dataSourceService.enablePlugin(testPluginId);
    Log.i(LOG_TAG, String.format("Checking if test plugin: %s is enabled", testPluginId));

    final boolean pluginEnabled = dataSourceService.isPluginEnabled(testPluginId);
    assertThat(pluginEnabled).isTrue();
    Log.i(LOG_TAG, "Plugin IS enabled");
  }

  @Test
  @DisplayName("Validate ALL data source plugins are returned by service")
  void getDataSourcePlugins() {

    final int expectedPluginCount = 3;

    // Retrieve all data source plugins
    final Collection<DataSourcePlugin> dataSourcePlugins = dataSourceService.getDataSourcePlugins();
    final int actualPluginCount = dataSourcePlugins.size();
    Log.i(LOG_TAG, String.format("Found: %s plugins", actualPluginCount));

    assertThat(actualPluginCount).isGreaterThanOrEqualTo(expectedPluginCount);
  }

  @Test
  @DisplayName("Validate correct # of ENABLED plugins is returned by service")
  void getEnabledPlugins() {

    final int expectedPluginCount = 2;

    final Collection<DataSourcePlugin> enabledPlugins = dataSourceService.getEnabledPlugins();
    final int actualPluginCount = enabledPlugins.size();
    Log.i(LOG_TAG, String.format("Found: %s enabled plugins", actualPluginCount));

    assertThat(actualPluginCount).isGreaterThanOrEqualTo(expectedPluginCount);
  }

  @Test
  @DisplayName("Validate that a DataSource can be added to the database")
  void addDataSource() {

    final DataSource<Event> testDataSource = testDataCreator.readTestJsonDataSource();
    Log.i(LOG_TAG, "Read test DataSource:\n" + testDataSource);
    final PatternKitPack testPatternKitPack =
        ((PlaintextDataSource<Event>) testDataSource).getPatternKitPack();
    assertThat(testPatternKitPack).isNotNull();

    final DataSource<Event> addedDataSource = dataSourceService.addDataSource(testDataSource);
    Log.i(LOG_TAG, "Added DataSource to database:\n" + addedDataSource);

    assertThat(addedDataSource).isNotNull();
    assertThat(addedDataSource.getBaseUri()).isEqualTo(testDataSource.getBaseUri());
    final PatternKitPack patternKitPack =
        ((PlaintextDataSource<Event>) addedDataSource).getPatternKitPack();
    assertThat(patternKitPack).isNotNull();
    final List<PatternKit<? extends Event>> eventPatternKits =
        patternKitPack.getPatternKitsFor(Event.class);
    assertThat(eventPatternKits).isNotNull();
    assertThat(eventPatternKits.size())
        .isEqualTo(testPatternKitPack.getPatternKitsFor(Event.class).size());
  }
}
