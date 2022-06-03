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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.*;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for all data source refresh service")
class DataSourceServiceTest {

  private static final Logger logger = LogManager.getLogger(DataSourceServiceTest.class);

  private static DataSourceService dataSourceService;
  private static TestDataCreator testDataCreator;
  private static EventService eventService;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull DataSourceService dataSourceService,
      @Autowired @NotNull TestDataCreator testDataCreator,
      @Autowired EventService eventService) {

    DataSourceServiceTest.dataSourceService = dataSourceService;
    DataSourceServiceTest.testDataCreator = testDataCreator;
    DataSourceServiceTest.eventService = eventService;
  }

  @Test
  @DisplayName("Refresh all data sources")
  void refreshAllDataSources() throws IOException {

    final int expectedEventCount = 1;

    final SnapshotRequest testRequest = SnapshotRequest.builder().build();
    logger.info("Testing Data Source Service refresh with Snapshot Request:\n{}", testRequest);

    final SnapshotRequest testResult = dataSourceService.refreshAllDataSources(testRequest);
    assertThat(testResult).isEqualTo(testRequest);

    // Ensure some data was collected by request
    final List<Event> events = eventService.fetchAll();
    final int actualEventCount = events.size();
    assertThat(actualEventCount).isGreaterThanOrEqualTo(expectedEventCount);
  }

  @Test
  @DisplayName("Validate that a DataSource can be added to the database")
  @SuppressWarnings("unchecked cast")
  void addDataSource() {

    final DataSource<Event> testDataSource = testDataCreator.readTestJsonDataSource();
    logger.info("Read test DataSource:\n{}", testDataSource);
    final PlaintextDataSource<?> testPlaintextDataSource = (PlaintextDataSource<?>) testDataSource;
    final List<PatternKit<?>> testPatternKitPack = testPlaintextDataSource.getPatternKits();
    assertThat(testPatternKitPack).isNotNull();

    final DataSource<Event> addedDataSource =
        (DataSource<Event>) dataSourceService.save(testDataSource);
    logger.info("Added DataSource to database:\n{}", addedDataSource);

    assertThat(addedDataSource).isNotNull();
    assertThat(addedDataSource.getBaseUri()).isEqualTo(testDataSource.getBaseUri());
    final PlaintextDataSource<Event> plaintextDataSource =
        (PlaintextDataSource<Event>) addedDataSource;
    final List<PatternKit<?>> patternKitPack = plaintextDataSource.getPatternKits();
    assertThat(patternKitPack).isNotNull();
    final List<PatternKit<? extends Event>> eventPatternKits =
        plaintextDataSource.getPatternKitsFor(Event.class);
    assertThat(eventPatternKits).isNotNull();
    assertThat(eventPatternKits.size())
        .isEqualTo(testPlaintextDataSource.getPatternKitsFor(Event.class).size());
  }
}
