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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class DataSourceService {

  private final SnapshotService snapshotService;
  private final DataSourceRepository dataSourceRepository;
  @Getter private final Collection<DataSourcePlugin> dataSourcePlugins;

  DataSourceService(
      SnapshotService snapshotService,
      DataSourceRepository dataSourceRepository,
      Collection<DataSourcePlugin> dataSourcePlugins) {

    this.snapshotService = snapshotService;
    this.dataSourceRepository = dataSourceRepository;
    this.dataSourcePlugins = dataSourcePlugins;
  }

  /**
   * Refresh all <b>enabled</b> data sources with the given Snapshot
   *
   * @param request Refresh request details
   * @return The SnapshotRequest, for additional processing
   */
  public SnapshotRequest refreshAllDataSources(@NotNull final SnapshotRequest request)
      throws IOException {

    for (DataSourcePlugin plugin : getEnabledPlugins()) {
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

  public List<DataSourcePlugin> getEnabledPlugins() {

    return dataSourcePlugins.stream()
        .filter(DataSourcePlugin::isEnabled)
        .collect(Collectors.toList());
  }

  private DataSourcePlugin getEnabledPlugin(UUID pluginId) {

    return getEnabledPlugins().stream()
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
   * Set a plugin as 'enabled' (active)
   *
   * @param pluginId The ID of the plugin
   */
  public void enablePlugin(@NotNull final UUID pluginId) {

    dataSourcePlugins.stream()
        .filter(plugin -> pluginId.equals(plugin.getPluginId()))
        .findFirst()
        .ifPresentOrElse(
            plugin -> plugin.setEnabled(true),
            () -> {
              throw new IllegalArgumentException("Could not find plugin with ID: " + pluginId);
            });
  }

  /**
   * Disable the given plugin, so it is excluded from data refresh requests
   *
   * @param pluginId The ID of the data plugin to disable
   */
  public void disablePlugin(@NotNull final UUID pluginId) {

    dataSourcePlugins.stream()
        .filter(plugin -> plugin.getPluginId().equals(pluginId))
        .findFirst()
        .ifPresentOrElse(
            plugin -> plugin.setEnabled(false),
            () -> {
              throw new IllegalArgumentException("Could not find plugin with ID: " + pluginId);
            });
  }

  /**
   * Determine if a given plugin is currently enabled (active)
   *
   * @param pluginId The ID of the plugin
   * @return true/false if currently active
   */
  public boolean isPluginEnabled(@NotNull final UUID pluginId) {

    return getEnabledPlugins().stream()
        .map(DataSourcePlugin::getPluginId)
        .anyMatch(pluginId::equals);
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
