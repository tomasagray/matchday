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

package self.me.matchday.unit.plugin.datasource.blogger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Event;
import self.me.matchday.model.Match;
import self.me.matchday.model.PatternKit;
import self.me.matchday.model.PlaintextDataSource;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.plugin.datasource.blogger.BloggerPlugin;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Verification tests for BloggerPlugin")
class BloggerPluginTest {

  private static final Logger logger = LogManager.getLogger(BloggerPluginTest.class);

  private final BloggerPlugin plugin;
  private final DataSource<Match> testDataSource;

  @Autowired
  public BloggerPluginTest(@NotNull TestDataCreator testDataCreator, BloggerPlugin bloggerPlugin)
      throws IOException {
    this.plugin = bloggerPlugin;
    this.testDataSource = testDataCreator.readTestJsonDataSource();
  }

  @Test
  @DisplayName("Validate plugin ID")
  void getPluginId() {
    final UUID expectedPluginId = UUID.fromString("64d08bc8-bd9f-11ea-b3de-0242ac130004");
    final UUID actualPluginId = plugin.getPluginId();
    logger.info("Testing plugin ID: {}", actualPluginId);
    assertThat(actualPluginId).isEqualTo(expectedPluginId);
  }

  @Test
  @DisplayName("Validate plugin title")
  void getTitle() {
    final String expectedTitle = "Blogger";
    final String actualTitle = plugin.getTitle();
    logger.info("Testing title: {}", actualTitle);
    assertThat(actualTitle).isEqualTo(expectedTitle);
  }

  @Test
  @DisplayName("Validate plugin description")
  void getDescription() {
    final String expectedTitle =
        "Reads a Blogger blog from either HTML or JSON sources, and makes it "
            + "available to the containing application as a POJO. Implements the DataSourcePlugin<> interface.";
    final String actualDescription = plugin.getDescription();
    logger.info("Testing description: {}", actualDescription);
    assertThat(actualDescription).isEqualTo(expectedTitle);
  }

  @Test
  @DisplayName("Ensure that a DataSource can be added via the BloggerPlugin")
  void addDataSource() {

    final int minimumPatternKitCount = 2;
    logger.info("Added datasource:\n{}", testDataSource);
    assertThat(testDataSource).isNotNull();
    assertThat(testDataSource.getBaseUri()).isNotNull();
    final List<PatternKit<? extends Match>> metadataPatterns =
        ((PlaintextDataSource<? extends Match>) testDataSource).getPatternKitsFor(Match.class);
    assertThat(metadataPatterns).isNotNull();
    assertThat(metadataPatterns.size()).isNotZero().isGreaterThanOrEqualTo(minimumPatternKitCount);
  }

  @Test
  @DisplayName("Get a Snapshot from the test HTML DataSource")
  void getHtmlSnapshot() throws IOException {

    logger.info(
        "Getting Snapshot with DataSource:\n{}  --  {}",
        testDataSource.getPluginId(),
        testDataSource.getBaseUri());
    final SnapshotRequest request = SnapshotRequest.builder().labels(List.of("Barcelona")).build();
    final Snapshot<? extends Match> testSnapshot = plugin.getSnapshot(request, testDataSource);
    assertThat(testSnapshot).isNotNull();

    final List<Match> testData = testSnapshot.getData().collect(Collectors.toList());
    final int eventCount = testData.size();
    logger.info("Found {} events\n", eventCount);
    assertThat(eventCount).isNotZero();
    testData.stream()
        .filter(Objects::nonNull)
        .forEach(
            event -> {
              logger.info("Got Event:\n{}", event);
              assertThat(event.getCompetition()).isNotNull();
              assertThat(event.getDate()).isNotNull().isAfter(LocalDateTime.MIN);
              final Set<VideoFileSource> fileSources = event.getFileSources();
              assertThat(fileSources).isNotNull();
              assertThat(fileSources.size()).isNotZero();
            });
  }

  @Test
  @DisplayName("Get a Snapshot from test JSON Blogger DataSource")
  void getJsonSnapshot() throws IOException {

    final SnapshotRequest request =
        SnapshotRequest.builder().labels(List.of("Barcelona")).maxResults(25).build();
    final Snapshot<? extends Event> snapshot = plugin.getSnapshot(request, testDataSource);
    assertThat(snapshot).isNotNull();

    final List<Event> events = snapshot.getData().collect(Collectors.toList());
    final int eventCount = events.size();
    logger.info("Events pulled from DataSource: {}", eventCount);
    assertThat(eventCount).isNotZero();

    events.forEach(
        event -> {
          logger.info("Read Event:\n{}", event);
          assertThat(event).isNotNull();
          assertThat(event.getCompetition()).isNotNull();
          assertThat(event.getDate()).isNotNull().isAfter(LocalDateTime.MIN);
        });
  }
}
