/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.startup;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import self.me.matchday.api.service.DataSourceService;
import self.me.matchday.model.SnapshotRequest;

/**
 * Class to pre-populate database with data from remote Galataman source.
 */
@Configuration
public class LoadEventData {

  private static final String LOG_TAG = "LoadEventData";

//  @Bean
  CommandLineRunner initEventSources(DataSourceService dataSourceService) {

    return args -> {
      // Create empty Snapshot request
      final SnapshotRequest snapshotRequest = SnapshotRequest.builder().build();

      // Refresh all data sources with default (empty) request
      dataSourceService.refreshDataSources(snapshotRequest);
    };
  }
}
