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
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Event;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFilePack;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.plugin.datasource.DataSourcePlugin;
import self.me.matchday.plugin.datasource.TestDataSourcePlugin;
import self.me.matchday.plugin.datasource.parsing.PatternKit;
import self.me.matchday.util.JsonParser;
import self.me.matchday.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for all data source refresh service")
class DataSourceServiceTest {

  private static final String LOG_TAG = "DataSourceServiceTest";

  private static TestDataCreator testDataCreator;
  private static DataSourceService dataSourceService;
  private static EventService eventService;
  private static DataSourcePlugin<Event> testDataSourcePlugin;

  @BeforeAll
  static void setUp(
      @Autowired TestDataCreator testDataCreator,
      @Autowired final TestDataSourcePlugin testDataSourcePlugin,
      @Autowired final @NotNull DataSourceService dataSourceService,
      @Autowired final EventService eventService) {

    DataSourceServiceTest.testDataCreator = testDataCreator;
    DataSourceServiceTest.dataSourceService = dataSourceService;
    DataSourceServiceTest.eventService = eventService;

    // Create test plugin & register
    DataSourceServiceTest.testDataSourcePlugin = testDataSourcePlugin;
    dataSourceService.getDataSourcePlugins().add(testDataSourcePlugin);
  }

  @AfterAll
  static void tearDown() {

    Log.i(LOG_TAG, "Deleting test data...");
    final boolean removed = dataSourceService.getDataSourcePlugins().remove(testDataSourcePlugin);
    assertThat(removed).isTrue();
  }

  @Test
  @DisplayName("Refresh all data sources")
  void refreshDataSources() {

    final int expectedEventCount = 1;

    final SnapshotRequest testRequest = SnapshotRequest.builder().build();
    Log.i(LOG_TAG, "Testing Data Source Service refresh with Snapshot Request:\n" + testRequest);

    final SnapshotRequest testResult = dataSourceService.refreshDataSources(testRequest);
    assertThat(testResult).isEqualTo(testRequest);

    // Ensure some data was collected by request
    final Optional<List<Event>> optionalEvents = eventService.fetchAllEvents();
    assertThat(optionalEvents).isPresent();
    final int actualEventCount = optionalEvents.get().size();
    assertThat(actualEventCount).isGreaterThanOrEqualTo(expectedEventCount);
  }

  @Test
  @DisplayName("Validate retrieval of a specific data source plugin by ID")
  void getDataSourcePlugin() {

    Log.i(LOG_TAG, "Attempting to retrieve test plugin: " + testDataSourcePlugin.getPluginId());
    // Retrieve test plugin
    final Optional<DataSourcePlugin<Event>> pluginOptional =
        dataSourceService.getDataSourcePlugin(
            DataSourceServiceTest.testDataSourcePlugin.getPluginId());

    assertThat(pluginOptional).isPresent();
    pluginOptional.ifPresent(
        plugin -> Log.i(LOG_TAG, "Successfully retrieved plugin: " + plugin.getTitle()));
  }

  @Test
  @DisplayName("Ensure plugins can be enabled")
  void enablePlugin() {

    dataSourceService.getDataSourcePlugins().add(testDataSourcePlugin);
    final UUID testPluginId = testDataSourcePlugin.getPluginId();

    // Attempt to enable plugin
    final boolean enabled = dataSourceService.enablePlugin(testPluginId);
    assertThat(enabled).isTrue();
    // Ensure plugin has been added to enabled plugins
    final Set<DataSourcePlugin<Event>> enabledPlugins = dataSourceService.getEnabledPlugins();
    final Optional<DataSourcePlugin<Event>> pluginOptional =
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
    final boolean disabled = dataSourceService.disablePlugin(testPluginId);
    assertThat(disabled).isTrue();

    // Ensure plugin is NOT enabled
    final Set<DataSourcePlugin<Event>> enabledPlugins = dataSourceService.getEnabledPlugins();
    final Optional<DataSourcePlugin<Event>> pluginOptional =
        enabledPlugins.stream()
            .filter(plugin -> testPluginId.equals(plugin.getPluginId()))
            .findAny();

    assertThat(pluginOptional).isNotPresent();
    // Re-enable for other tests
    final boolean enabled = dataSourceService.enablePlugin(testPluginId);
    assertThat(enabled).isTrue();
  }

  @Test
  @DisplayName("Validate plugin is correctly assessed as enabled")
  void isPluginEnabled() {

    final UUID testPluginId = testDataSourcePlugin.getPluginId();
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
    final Set<DataSourcePlugin<Event>> dataSourcePlugins = dataSourceService.getDataSourcePlugins();
    final int actualPluginCount = dataSourcePlugins.size();
    Log.i(LOG_TAG, String.format("Found: %s plugins", actualPluginCount));

    assertThat(actualPluginCount).isGreaterThanOrEqualTo(expectedPluginCount);
  }

  @Test
  @DisplayName("Validate correct # of ENABLED plugins is returned by service")
  void getEnabledPlugins() {

    final int expectedPluginCount = 2;

    final Set<DataSourcePlugin<Event>> enabledPlugins = dataSourceService.getEnabledPlugins();
    final int actualPluginCount = enabledPlugins.size();
    Log.i(LOG_TAG, String.format("Found: %s enabled plugins", actualPluginCount));

    assertThat(actualPluginCount).isGreaterThanOrEqualTo(expectedPluginCount);
  }

  @Test
  @DisplayName("Validate that a DataSource can be added to the database")
  void addDataSource() {

//    createTestFile();
    final DataSource testDataSource = testDataCreator.readTestJsonDataSource();
    Log.i(LOG_TAG, "Read test DataSource:\n" + testDataSource);
    final DataSource addedDataSource = dataSourceService.addDataSource(testDataSource);
    Log.i(LOG_TAG, "Added DataSource to database:\n" + addedDataSource);

    assertThat(addedDataSource).isNotNull();
    assertThat(addedDataSource.getBaseUri()).isEqualTo(testDataSource.getBaseUri());
    assertThat(addedDataSource.getPatternKits().size())
        .isEqualTo(testDataSource.getPatternKits().size());
  }

  private static void createTestFile() {
    final PatternKit<Event> eventPatternKit = testDataCreator.createEventPatternKit();
    final PatternKit<VideoFileSource> fileSourcePatternKit =
        testDataCreator.createFileSourcePatternKit();
    final PatternKit<VideoFilePack> videoFilePackPatternKit =
        testDataCreator.createVideoFilePackPatternKit();
    final PatternKit<VideoFile> videoFilePatternKit = testDataCreator.createVideoFilePatternKit();
    final PatternKit<URL> urlPatternKit = testDataCreator.createUrlPatternKit();
    final List<PatternKit<?>> patternKits =
        List.of(
            eventPatternKit,
            fileSourcePatternKit,
            videoFilePackPatternKit,
            videoFilePatternKit,
            urlPatternKit);
    final DataSource dataSource =
        new DataSource(
            UUID.fromString("37149b7c-4dae-48c2-997a-a7427628b408"),
            URI.create("https://localhost:8081/"),
            patternKits);
    final String json = JsonParser.toJson(dataSource);

    try (BufferedWriter writer =
        new BufferedWriter(
            new FileWriter(
                "C:\\Users\\Tomas\\Projects\\Source\\IdeaProjects\\matchday\\src\\test\\resources\\test_json_blogger_datasource.json"))) {
      writer.write(json);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
