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

package self.me.matchday.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Event;
import self.me.matchday.model.PatternKitPack;
import self.me.matchday.model.PlaintextDataSource;
import self.me.matchday.util.JsonParser;
import self.me.matchday.util.Log;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validation of DataSource repository")
class DataSourceRepositoryTest {

  private static final String LOG_TAG = "DataSourceRepositoryTest";

  private static TestDataCreator testDataCreator;
  private static DataSourceRepository repository;

  @BeforeAll
  static void setup(
      @Autowired TestDataCreator testDataCreator, @Autowired DataSourceRepository repository) {
    DataSourceRepositoryTest.testDataCreator = testDataCreator;
    DataSourceRepositoryTest.repository = repository;
  }

  @Test
  @DisplayName("Ensure DataSource is not corrupted when saving to DB")
  void testSaveToDatabase() {
    final DataSource<Event> dataSource = testDataCreator.readTestJsonDataSource();
    final DataSource<Event> savedDataSource = repository.save(dataSource);
    final PatternKitPack readPkp = ((PlaintextDataSource<Event>) dataSource).getPatternKitPack();
    final PatternKitPack savedPkp =
        ((PlaintextDataSource<Event>) savedDataSource).getPatternKitPack();
    // update ID
    dataSource.setDataSourceId(savedDataSource.getDataSourceId());

    Log.i(LOG_TAG, "Saved DataSource: " + savedDataSource);
    assertThat(savedDataSource).isNotNull();
    assertThat(savedDataSource.getDataSourceId()).isEqualTo(dataSource.getDataSourceId());
    assertThat(savedDataSource.getPluginId()).isEqualTo(dataSource.getPluginId());
    assertThat(savedDataSource.getClazz()).isEqualTo(dataSource.getClazz());
    assertThat(savedDataSource.getBaseUri()).isEqualTo(dataSource.getBaseUri());
    assertThat(savedPkp.getPatternKits().size()).isEqualTo(readPkp.getPatternKits().size());
  }

  @SuppressWarnings("unchecked cast")
  @Test
  @DisplayName("Ensure valid data read when fetching by Plugin ID")
  @Transactional
  void testFetchByPluginId() {

    final PlaintextDataSource<Event> eventDataSource =
        (PlaintextDataSource<Event>) testDataCreator.readTestJsonDataSource();
    final PlaintextDataSource<Event> savedDataSource = repository.save(eventDataSource);
    Log.i(LOG_TAG, "Saved DataSource:\n" + savedDataSource);

    Log.i(LOG_TAG, "Attempting to fetch test DataSource by Plugin ID...");
    final List<DataSource<?>> dataSourcesByPluginId =
        repository.findDataSourcesByPluginId(eventDataSource.getPluginId());
    final int dataSourceCount = dataSourcesByPluginId.size();
    assertThat(dataSourceCount).isNotZero();

    Log.i(LOG_TAG, String.format("Found: %d DataSources...", dataSourceCount));
    final PlaintextDataSource<Event> testDataSource =
        (PlaintextDataSource<Event>) dataSourcesByPluginId.get(0);
    Log.i(LOG_TAG, "Testing retrieved DataSource:\n" + JsonParser.toJson(testDataSource));

    assertThat(testDataSource.getPatternKitPack().getPatternKitsFor(Event.class).size())
        .isEqualTo(eventDataSource.getPatternKitPack().getPatternKitsFor(Event.class).size());
  }
}
