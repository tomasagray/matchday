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

package net.tomasbot.matchday.unit.plugin.datasource.blogger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import net.tomasbot.matchday.model.*;
import net.tomasbot.matchday.plugin.datasource.blogger.BloggerPlugin;
import net.tomasbot.matchday.util.JsonParser;
import net.tomasbot.matchday.util.ResourceFileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Verification tests for BloggerPlugin")
@Disabled
class BloggerPluginTest {

  private static final Logger logger = LogManager.getLogger(BloggerPluginTest.class);

  private final BloggerPlugin plugin;
  private final DataSource<BloggerTestEntity> testDataSource;

  @Autowired
  public BloggerPluginTest(BloggerPlugin bloggerPlugin) throws IOException {
    this.plugin = bloggerPlugin;
    this.testDataSource = readTestDataSource();
  }

  private static DataSource<BloggerTestEntity> readTestDataSource() throws IOException {
    final String filepath = "data/blogger/blogger_test_datasource.json";
    String data = ResourceFileReader.readTextResource(filepath);
    final Type type = new TypeReference<PlaintextDataSource<BloggerTestEntity>>() {}.getType();
    return JsonParser.fromJson(data, type);
  }

  private static void testEntities(@NotNull List<BloggerTestEntity> entities) {
    int count = entities.size();
    logger.info("Found: {} BloggerTestEntities...", count);
    assertThat(count).isNotZero();
    entities.forEach(
        entity -> {
          logger.info("Found BloggerTestEntity: {}", entity);
          assertThat(entity.getTitle()).isNotNull().isNotEmpty();
          assertThat(entity.getText()).isNotNull().isNotEmpty();
          assertThat(entity.getPublished()).isNotNull();
        });
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
    final int minimumPatternKitCount = 1;
    logger.info("Added datasource:\n{}", testDataSource);
    assertThat(testDataSource).isNotNull();
    assertThat(testDataSource.getBaseUri()).isNotNull();
    final List<PatternKit<? extends BloggerTestEntity>> metadataPatterns =
        ((PlaintextDataSource<? extends BloggerTestEntity>) testDataSource)
            .getPatternKitsFor(BloggerTestEntity.class);
    assertThat(metadataPatterns).isNotNull();
    assertThat(metadataPatterns.size()).isNotZero().isGreaterThanOrEqualTo(minimumPatternKitCount);
  }

  @Test
  @DisplayName("Get a Snapshot from a test Blogger blog (HTML)")
  void getKnownTestSnapshot() throws IOException {
    logger.info(
        "Getting Snapshot with DataSource: {}  --  {}",
        testDataSource.getPluginId(),
        testDataSource.getBaseUri());
    final SnapshotRequest request = SnapshotRequest.builder().build();
    final Snapshot<? extends BloggerTestEntity> testSnapshot =
        plugin.getSnapshot(request, testDataSource);
    assertThat(testSnapshot).isNotNull();

    final List<BloggerTestEntity> testData = testSnapshot.getData().collect(Collectors.toList());
    final int eventCount = testData.size();
    logger.info("Found {} BloggerTestEntities...", eventCount);
    assertThat(eventCount).isNotZero();
    testData.stream()
        .filter(Objects::nonNull)
        .forEach(
            entity -> {
              logger.info("Found Entity: {}", entity);
              assertThat(entity.getTitle()).isNotNull().isNotEmpty();
              assertThat(entity.getText()).isNotNull().isNotEmpty();
            });
  }

  @Test
  @DisplayName("Get a date-limited Snapshot from test blog")
  void dateLimitedSnapshotTest() throws IOException {
    // given
    final LocalDateTime limit = LocalDateTime.of(2023, 9, 1, 0, 0);
    logger.info("Getting posts until: {}", limit);

    // when
    SnapshotRequest request = SnapshotRequest.builder().startDate(limit).build();
    logger.info("Getting Snapshot with request: {}", request);
    Snapshot<BloggerTestEntity> snapshot = plugin.getSnapshot(request, testDataSource);
    List<BloggerTestEntity> entities = snapshot.getData().toList();

    // then
    testEntities(entities);

    ArrayList<BloggerTestEntity> modifiable = new ArrayList<>(entities);
    modifiable.sort(Comparator.comparing(BloggerTestEntity::getPublished));
    BloggerTestEntity leastRecent = modifiable.get(0);
    logger.info("Least recent post: {}", leastRecent);
    assertThat(leastRecent.getPublished()).isBetween(limit.minusWeeks(1), limit);
  }

  @Test
  @DisplayName("Validate query of Blogger blog with labels query")
  void testLabelBloggerQuery() throws IOException {
    // given
    final List<String> labels = List.of("label #1");
    logger.info("Getting posts with label: {}", labels);

    // when
    SnapshotRequest request = SnapshotRequest.builder().labels(labels).build();
    logger.info("Getting BloggerTest Snapshot with request: {}", request);
    Snapshot<BloggerTestEntity> snapshot = plugin.getSnapshot(request, testDataSource);
    List<BloggerTestEntity> entities = snapshot.getData().toList();

    // then
    testEntities(entities);
  }
}
