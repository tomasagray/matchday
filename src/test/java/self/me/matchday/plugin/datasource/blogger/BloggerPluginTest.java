/*
 * Copyright (c) 2021.
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Event;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.model.video.VideoSourceMetadataPatternKit;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Verification tests for BloggerPlugin")
class BloggerPluginTest {

  private static final String LOG_TAG = "BloggerPluginTest";

  private static BloggerPlugin plugin;
  private static DataSource testDataSource;

  @BeforeAll
  static void setUp(@Autowired BloggerPlugin bloggerPlugin) {
    plugin = bloggerPlugin;
    final DataSource dataSource = TestDataCreator.readTestDataSource();

    Log.i(LOG_TAG, "Adding test datasource to DB...");
    testDataSource =
        plugin.addDataSource(dataSource.getBaseUri(), dataSource.getMetadataPatterns());
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

    Log.i(LOG_TAG, "Added datasource:\n" + testDataSource);
    assertThat(testDataSource).isNotNull();
    assertThat(testDataSource.getBaseUri()).isNotNull();
    final List<VideoSourceMetadataPatternKit> metadataPatterns =
        testDataSource.getMetadataPatterns();
    assertThat(metadataPatterns).isNotNull();
    assertThat(metadataPatterns.size()).isNotZero();
  }

  @Test
  @Disabled
  void getAllSnapshots() {
    // todo - getAllSnapshots test
  }

  @Test
  @DisplayName("Get a Snapshot from the test DataSource")
  void getSnapshot() throws IOException {

    Log.i(LOG_TAG, "Getting Snapshot with DataSource:\n" + testDataSource.getId());
    final SnapshotRequest request = SnapshotRequest.builder().build();
    final Snapshot<Event> testSnapshot = plugin.getSnapshot(request, testDataSource);
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
}
