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

package self.me.matchday.api.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.model.Event;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.DataSourcePlugin;
import self.me.matchday.util.Log;

@Service
public class DataSourceService {

  private static final String LOG_TAG = "DataSourceService";

  private final List<DataSourcePlugin<Stream<Event>>> dataSourcePlugins;
  private final EventService eventService;

  @Autowired
  DataSourceService(@NotNull final List<DataSourcePlugin<Stream<Event>>> dataSourcePlugins,
      @NotNull final EventService eventService) {

    this.dataSourcePlugins = dataSourcePlugins;
    this.eventService = eventService;
  }

  public SnapshotRequest refreshDataSources(@NotNull final SnapshotRequest snapshotRequest) {

    // Refresh each data source plugin
    dataSourcePlugins
        .forEach(plugin -> {
          try {
            final Snapshot<Stream<Event>> snapshot = plugin.getSnapshot(snapshotRequest);
            // Save Snapshot data to database
            snapshot
                .getData()
                .forEach(eventService::saveEvent);

          } catch (IOException | RuntimeException e) {
            Log.e(LOG_TAG,
                String.format("Could not refresh data from plugin: %s with SnapshotRequest: %s",
                    plugin.getTitle(), snapshotRequest), e);
          }
        });

    return snapshotRequest;
  }
}
