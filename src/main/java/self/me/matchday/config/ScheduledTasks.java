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

package self.me.matchday.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.DataSourceService;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.util.Log;

/**
 * Regularly run tasks. Configuration in external properties file.
 */
@Component
@PropertySource("classpath:scheduled-tasks.properties")
public class ScheduledTasks {

  private static final String LOG_TAG = "ScheduledTasks";

  private final DataSourceService dataSourceService;

  @Autowired
  public ScheduledTasks(@NotNull final DataSourceService dataSourceService) {
    this.dataSourceService = dataSourceService;
  }

  @Scheduled(cron = "${scheduled-tasks.cron.refresh-event-data}")
  public void refreshEventData() {

    Log.i(LOG_TAG, "Refreshing all data sources...");
    // Create empty SnapshotRequest
    final SnapshotRequest snapshotRequest = SnapshotRequest.builder().build();
    // Refresh data sources
    dataSourceService.refreshDataSources(snapshotRequest);
  }
}
