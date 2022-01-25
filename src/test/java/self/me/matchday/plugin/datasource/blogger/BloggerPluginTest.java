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

package self.me.matchday.plugin.datasource.blogger;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.api.service.DataSourceService;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Event;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.parsing.PatternKit;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Verification tests for BloggerPlugin")
class BloggerPluginTest {

  private static final String LOG_TAG = "BloggerPluginTest";

  private static BloggerPlugin plugin;
  private static DataSource testHtmlDataSource;
  private static DataSource testJsonDataSource;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull TestDataCreator testDataCreator,
      @Autowired BloggerPlugin bloggerPlugin,
      @Autowired @NotNull DataSourceService dataSourceService) {

    plugin = bloggerPlugin;
    final DataSource testHtmlDataSource = testDataCreator.readTestHtmlDataSource();
    final DataSource testJsonDataSource = testDataCreator.readTestJsonDataSource();

    Log.i(LOG_TAG, "Adding test datasource to DB...");
    BloggerPluginTest.testHtmlDataSource = dataSourceService.addDataSource(testHtmlDataSource);
    BloggerPluginTest.testJsonDataSource = dataSourceService.addDataSource(testJsonDataSource);
  }

  @Test
  @DisplayName("Validate plugin ID")
  void getPluginId() {
    final UUID expectedPluginId = UUID.fromString("64d08bc8-bd9f-11ea-b3de-0242ac130004");
    final UUID actualPluginId = plugin.getPluginId();
    Log.i(LOG_TAG, "Testing plugin ID: " + actualPluginId);
    assertThat(actualPluginId).isEqualTo(expectedPluginId);
  }

  @Test
  @DisplayName("Validate plugin title")
  void getTitle() {
    final String expectedTitle = "Blogger";
    final String actualTitle = plugin.getTitle();
    Log.i(LOG_TAG, "Testing title: " + actualTitle);
    assertThat(actualTitle).isEqualTo(expectedTitle);
  }

  @Test
  @DisplayName("Validate plugin description")
  void getDescription() {
    final String expectedTitle =
        "Reads a Blogger blog from either HTML or JSON sources, and makes it "
            + "available to the containing application as a POJO. Implements the DataSourcePlugin<> interface.";
    final String actualDescription = plugin.getDescription();
    Log.i(LOG_TAG, "Testing description: " + actualDescription);
    assertThat(actualDescription).isEqualTo(expectedTitle);
  }

  @Test
  @DisplayName("Ensure that a DataSource can be added via the BloggerPlugin")
  void addDataSource() {

    Log.i(LOG_TAG, "Added datasource:\n" + testHtmlDataSource);
    assertThat(testHtmlDataSource).isNotNull();
    assertThat(testHtmlDataSource.getBaseUri()).isNotNull();
    final List<PatternKit<?>> metadataPatterns = testHtmlDataSource.getPatternKits();
    assertThat(metadataPatterns).isNotNull();
    assertThat(metadataPatterns.size()).isNotZero();
  }

  @Test
  @DisplayName("Validate getting Snapshot of all plugin DataSources")
  void getAllSnapshots() {

    final int minEventCount = 5;
    Log.i(LOG_TAG, "Attempting to get Snapshots from all Blogger DataSources...");

    final SnapshotRequest request = SnapshotRequest.builder().build();
    final Snapshot<? extends Event> snapshot = plugin.getAllSnapshots(request);

    final Stream<? extends Event> data = snapshot.getData();
    assertThat(data).isNotNull();

    final AtomicInteger eventCounter = new AtomicInteger();
    data.forEach(
        event -> {
          Log.i(LOG_TAG, "Refresh got Event:\n" + event);
          assertThat(event).isNotNull();
          assertThat(event.getCompetition()).isNotNull();
          assertThat(event.getDate()).isNotNull().isAfter(LocalDateTime.MIN);
          eventCounter.getAndIncrement();
        });

    final int eventCount = eventCounter.get();
    Log.i(LOG_TAG, String.format("Pulled: %d Events", eventCount));
    assertThat(eventCount).isGreaterThanOrEqualTo(minEventCount);
  }

  @Test
  @DisplayName("Get a Snapshot from the test HTML DataSource")
  void getSnapshot() throws IOException {

    Log.i(LOG_TAG, "Getting Snapshot with DataSource:\n" + testHtmlDataSource.getId());
    final SnapshotRequest request = SnapshotRequest.builder().build();
    final Snapshot<? extends Event> testSnapshot = plugin.getSnapshot(request, testHtmlDataSource);
    assertThat(testSnapshot).isNotNull();

    final List<Event> testData = testSnapshot.getData().collect(Collectors.toList());
    final int eventCount = testData.size();
    Log.i(LOG_TAG, String.format("Found %d events%nData:%n%s", eventCount, testData));
    assertThat(eventCount).isNotZero();
    testData.stream()
        .filter(Objects::nonNull)
        .forEach(
            event -> {
              Log.i(LOG_TAG, "Got Event:\n" + event);
              assertThat(event.getCompetition()).isNotNull();
              assertThat(event.getDate()).isNotNull().isAfter(LocalDateTime.MIN);
            });
  }

  @Test
  @DisplayName("Validate handling of JSON Blogger DataSource")
  void getJsonSnapshot() throws IOException {

    Log.i(LOG_TAG, "Getting Snapshot of test DataSource: " + testJsonDataSource);
    final SnapshotRequest request = SnapshotRequest.builder().maxResults(25).build();
    final Snapshot<? extends Event> snapshot = plugin.getSnapshot(request, testJsonDataSource);
    assertThat(snapshot).isNotNull();

    final List<Event> events = snapshot.getData().collect(Collectors.toList());
    final int eventCount = events.size();
    Log.i(LOG_TAG, "Events pulled from DataSource: " + eventCount);
    assertThat(eventCount).isNotZero();

    events.forEach(
        event -> {
          Log.i(LOG_TAG, "Read Event:\n" + event);
          assertThat(event).isNotNull();
          assertThat(event.getCompetition()).isNotNull();
          assertThat(event.getDate()).isNotNull().isAfter(LocalDateTime.MIN);
        });
  }
}
