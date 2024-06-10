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

package self.me.matchday.unit.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.TestDataCreator;
import self.me.matchday.db.DataSourceRepository;
import self.me.matchday.model.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validation of DataSource repository")
class DataSourceRepositoryTest {

  private static final Logger logger = LogManager.getLogger(DataSourceRepositoryTest.class);

  private final TestDataCreator testDataCreator;
  private final DataSourceRepository repository;

  @Autowired
  public DataSourceRepositoryTest(
      TestDataCreator testDataCreator, DataSourceRepository repository) {
    this.testDataCreator = testDataCreator;
    this.repository = repository;
  }

  @Test
  @DisplayName("Ensure DataSource is not corrupted when saving to DB")
  void testSaveToDatabase() throws IOException {
    final DataSource<Match> dataSource = testDataCreator.readTestLiveDataSource();
    final DataSource<Match> savedDataSource = repository.save(dataSource);
    final PlaintextDataSource<Match> plaintextDataSource = (PlaintextDataSource<Match>) dataSource;
    final List<PatternKit<?>> readPkp = plaintextDataSource.getPatternKits();
    final List<PatternKit<?>> savedPkp =
        ((PlaintextDataSource<Match>) savedDataSource).getPatternKits();
    // update ID
    dataSource.setDataSourceId(savedDataSource.getDataSourceId());

    logger.info("Saved DataSource: {}", savedDataSource);
    assertThat(savedDataSource).isNotNull();
    assertThat(savedDataSource.getDataSourceId()).isEqualTo(dataSource.getDataSourceId());
    assertThat(savedDataSource.getPluginId()).isEqualTo(dataSource.getPluginId());
    assertThat(savedDataSource.getClazz()).isEqualTo(dataSource.getClazz());
    assertThat(savedDataSource.getBaseUri()).isEqualTo(dataSource.getBaseUri());
    assertThat(savedPkp.size()).isEqualTo(readPkp.size());
    logger.info("Saved Data Source was not corrupted");
  }

  @SuppressWarnings("unchecked cast")
  @Test
  @DisplayName("Ensure valid data read when fetching by Plugin ID")
  @Transactional
  void testFetchByPluginId() throws IOException {
    final PlaintextDataSource<Match> eventDataSource =
        (PlaintextDataSource<Match>) testDataCreator.readTestLiveDataSource();
    final PlaintextDataSource<Match> savedDataSource = repository.save(eventDataSource);
    logger.info("Saved DataSource:\n{}", savedDataSource);

    UUID pluginId = eventDataSource.getPluginId();
    logger.info("Attempting to fetch test DataSource by Plugin ID: {}...", pluginId);
    final List<DataSource<?>> dataSourcesByPluginId =
        repository.findDataSourcesByPluginId(pluginId);
    final int dataSourceCount = dataSourcesByPluginId.size();
    assertThat(dataSourceCount).isNotZero();

    logger.info("Found: {} DataSources...", dataSourceCount);
    // use most recent data source
    final PlaintextDataSource<Event> testDataSource =
        (PlaintextDataSource<Event>) dataSourcesByPluginId.get(dataSourceCount - 1);
    logger.info("Testing retrieved DataSource:\n{}", testDataSource);

    assertThat(testDataSource.getPatternKitsFor(Event.class).size())
        .isEqualTo(eventDataSource.getPatternKitsFor(Event.class).size());
  }
}
