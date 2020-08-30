/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
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
