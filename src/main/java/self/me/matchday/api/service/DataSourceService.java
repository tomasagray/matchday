/*
 * Copyright (c) 2021.
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

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.model.Event;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.DataSourcePlugin;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class DataSourceService {

  private static final String LOG_TAG = "DataSourceService";

  @Getter private final Set<DataSourcePlugin<Event>> dataSourcePlugins;
  @Getter private final Set<DataSourcePlugin<Event>> enabledPlugins = new HashSet<>();
  private final EventService eventService;

  @Autowired
  DataSourceService(
      @NotNull final Set<DataSourcePlugin<Event>> dataSourcePlugins,
      @NotNull final EventService eventService) {

    this.dataSourcePlugins = dataSourcePlugins;
    // all enabled by default
    this.enabledPlugins.addAll(dataSourcePlugins);
    this.eventService = eventService;
  }

  /**
   * Refresh all data sources (Event source plugins) with the given Snapshot
   *
   * @param snapshotRequest Refresh request details
   * @return The SnapshotRequest, for additional processing
   */
  public SnapshotRequest refreshDataSources(@NotNull final SnapshotRequest snapshotRequest) {

    // Refresh each data source plugin
    enabledPlugins.forEach(
        plugin -> {
          try {
            final Snapshot<Event> snapshot = plugin.getSnapshot(snapshotRequest);
            // Save Snapshot data to database
            snapshot.getData().forEach(eventService::saveEvent);

          } catch (IOException | RuntimeException e) {
            Log.e(
                LOG_TAG,
                String.format(
                    "Could not refresh data from plugin: %s with SnapshotRequest: %s",
                    plugin.getTitle(), snapshotRequest),
                e);
          }
        });

    return snapshotRequest;
  }

  /**
   * Returns an Optional containing the requested plugin, if it exists
   *
   * @param pluginId The ID of the requested plugin
   * @return An Optional which may contain the requested plugin
   */
  public Optional<DataSourcePlugin<Event>> getDataSourcePlugin(@NotNull final UUID pluginId) {

    return dataSourcePlugins.stream()
        .filter(plugin -> pluginId.equals(plugin.getPluginId()))
        .findFirst();
  }

  /**
   * Set a plugin at 'enabled' (active) by adding it to the enabledPlugins List
   *
   * @param pluginId The ID of the plugin
   * @return True/false if the plugin was successfully enabled
   */
  public boolean enablePlugin(@NotNull final UUID pluginId) {

    Log.i(LOG_TAG, "Attempting to enable plugin: " + pluginId);

    // Find requested plugin
    final Optional<DataSourcePlugin<Event>> pluginOptional =
        dataSourcePlugins.stream()
            .filter(plugin -> pluginId.equals(plugin.getPluginId()))
            .findFirst();
    if (pluginOptional.isPresent()) {
      // Plugin found; enable
      final DataSourcePlugin<Event> plugin = pluginOptional.get();
      final boolean added = enabledPlugins.add(plugin);
      Log.i(
          LOG_TAG, String.format("Successfully enabled plugin: %s? %s", plugin.getTitle(), added));
      return added;
    } else {
      Log.i(LOG_TAG, "Could not find plugin with ID: " + pluginId);
    }
    return false;
  }

  /**
   * Disable the given plugin, so it is excluded from data refresh requests
   *
   * @param pluginId The ID of the data plugin to disable
   * @return True/false if the given plugin was successfully disabled
   */
  public boolean disablePlugin(@NotNull final UUID pluginId) {

    Log.i(LOG_TAG, "Attempting to disable plugin: " + pluginId);

    // Find the requested plugin
    final Optional<DataSourcePlugin<Event>> pluginOptional =
        enabledPlugins.stream().filter(plugin -> pluginId.equals(plugin.getPluginId())).findFirst();
    if (pluginOptional.isPresent()) {
      // Remove from enabled plugins
      final DataSourcePlugin<Event> plugin = pluginOptional.get();
      final boolean removed = enabledPlugins.remove(plugin);
      Log.i(LOG_TAG, String.format("Disabled plugin: %s? %s", plugin.getTitle(), removed));
      return removed;
    } else {
      Log.i(LOG_TAG, "Could not find plugin with ID: " + pluginId);
    }
    return false;
  }

  /**
   * Determine if a given plugin is currently enabled (active)
   *
   * @param pluginId The ID of the plugin
   * @return true/false if currently active
   */
  public boolean isPluginEnabled(@NotNull final UUID pluginId) {
    return enabledPlugins.stream().anyMatch(plugin -> pluginId.equals(plugin.getPluginId()));
  }
}
