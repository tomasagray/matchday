/*
 * Copyright (c) 2020.
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

package self.me.matchday.startup;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import self.me.matchday.api.service.DataSourceService;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.util.Log;

/**
 * Class to refresh database with data from all remote sources on startup.
 */
@Configuration
public class InitEventData {

  private static final String LOG_TAG = "InitEventData";
  private static final String INIT_MSG =
      "Performing first time data refresh of all data sources using empty SnapshotRequest:\n%s";


  @Bean
  CommandLineRunner initialEventDataLoad(DataSourceService dataSourceService) {

    return args -> {
      // Create empty Snapshot request
      final SnapshotRequest snapshotRequest = SnapshotRequest.builder().build();
      Log.i(LOG_TAG, String.format(INIT_MSG, snapshotRequest));

      // Refresh all data sources with default (empty) request
      dataSourceService.refreshDataSources(snapshotRequest);
    };
  }
}
