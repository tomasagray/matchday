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

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.DataSourceRepository;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.DataSourcePlugin;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.util.*;

@Service
@Transactional
public class DataSourceService {

  private static final String LOG_TAG = "DataSourceService";

  private final SnapshotService snapshotService;
  private final DataSourceRepository dataSourceRepository;
  @Getter private final Set<DataSourcePlugin> dataSourcePlugins;
  @Getter private final Set<DataSourcePlugin> enabledPlugins = new HashSet<>();

  DataSourceService(
      SnapshotService snapshotService,
      DataSourceRepository dataSourceRepository,
      Set<DataSourcePlugin> dataSourcePlugins) {

    this.snapshotService = snapshotService;
    this.dataSourceRepository = dataSourceRepository;
    this.dataSourcePlugins = dataSourcePlugins;
    // all enabled by default
    this.enabledPlugins.addAll(dataSourcePlugins);
  }

  /**
   * Refresh all data sources (Event source plugins) with the given Snapshot
   *
   * @param request Refresh request details
   * @return The SnapshotRequest, for additional processing
   */
  public SnapshotRequest refreshAllDataSources(@NotNull final SnapshotRequest request)
      throws IOException {

    for (DataSourcePlugin plugin : enabledPlugins) {
      refreshDataSourcesForPlugin(request, plugin);
    }
    return request;
  }

  public void refreshDataSourcesForPlugin(
      @NotNull SnapshotRequest request, @NotNull DataSourcePlugin plugin) throws IOException {

    final List<DataSource<?>> dataSources =
        dataSourceRepository.findDataSourcesByPluginId(plugin.getPluginId());
    for (final DataSource<?> dataSource : dataSources) {
      refreshDataSource(request, dataSource);
    }
  }

  public <T> void refreshDataSource(
      @NotNull SnapshotRequest request, @NotNull DataSource<T> dataSource) throws IOException {

    final DataSourcePlugin dataSourcePlugin = getEnabledPlugin(dataSource.getPluginId());
    final Snapshot<T> snapshot = dataSourcePlugin.getSnapshot(request, dataSource);
    snapshotService.saveSnapshot(snapshot, dataSource.getClazz());
  }

  public DataSourcePlugin getEnabledPlugin(UUID pluginId) {

    return enabledPlugins.stream()
        .filter(plugin -> plugin.getPluginId().equals(pluginId))
        .findFirst()
        .orElseThrow(
            () -> {
              final Optional<DataSourcePlugin> pluginOptional = getDataSourcePlugin(pluginId);
              if (pluginOptional.isPresent()) {
                return new IllegalArgumentException(
                    String.format("DataSourcePlugin: %s is disabled", pluginId));
              } else {
                return new IllegalArgumentException(
                    "No enabled DataSourcePlugin with ID matching: " + pluginId);
              }
            });
  }

  /**
   * Returns an Optional containing the requested plugin, if it exists
   *
   * @param pluginId The ID of the requested plugin
   * @return An Optional which may contain the requested plugin
   */
  public Optional<DataSourcePlugin> getDataSourcePlugin(@NotNull final UUID pluginId) {

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
    final Optional<DataSourcePlugin> pluginOptional =
        dataSourcePlugins.stream()
            .filter(plugin -> pluginId.equals(plugin.getPluginId()))
            .findFirst();
    return pluginOptional
        .map(plugin -> enabledPlugins.contains(plugin) || enabledPlugins.add(plugin))
        .orElse(false);
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
    final Optional<DataSourcePlugin> pluginOptional =
        enabledPlugins.stream().filter(plugin -> pluginId.equals(plugin.getPluginId())).findFirst();
    if (pluginOptional.isPresent()) {
      // Remove from enabled plugins
      final DataSourcePlugin plugin = pluginOptional.get();
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

  public <T> DataSource<T> addDataSource(@NotNull final DataSource<T> dataSource) {

    final String errMsg =
        String.format(
            "Could not save DataSource with ID: %s; no DataSourcePlugin with ID: %s",
            dataSource.getDataSourceId(), dataSource.getPluginId());

    return this.getDataSourcePlugin(dataSource.getPluginId())
        .map(plugin -> saveDataSource(dataSource, plugin))
        .orElseThrow(() -> new IllegalArgumentException(errMsg));
  }

  @NotNull
  private <T> DataSource<T> saveDataSource(
      @NotNull DataSource<T> dataSource, @NotNull DataSourcePlugin plugin) {
    plugin.validateDataSource(dataSource);
    return dataSourceRepository.saveAndFlush(dataSource);
  }

  public Optional<DataSource<?>> getDataSourceById(@NotNull UUID id) {
    return dataSourceRepository.findById(id);
  }

  public List<DataSource<?>> getDataSourcesForPlugin(@NotNull UUID pluginId) {
    return dataSourceRepository.findDataSourcesByPluginId(pluginId);
  }

  // TODO: update DataSource, delete DataSource
}
