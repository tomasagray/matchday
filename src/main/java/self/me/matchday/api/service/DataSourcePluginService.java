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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.matchday.model.DataSource;
import self.me.matchday.plugin.datasource.DataSourcePlugin;

@Service
public class DataSourcePluginService {

  private final Collection<DataSourcePlugin> plugins;

  DataSourcePluginService(Collection<DataSourcePlugin> plugins) {
    this.plugins = plugins;
  }

  public Collection<DataSourcePlugin> getDataSourcePlugins() {
    return this.plugins;
  }

  /**
   * Returns an Optional containing the requested plugin, if it exists
   *
   * @param pluginId The ID of the requested plugin
   * @return An Optional which may contain the requested plugin
   */
  public Optional<DataSourcePlugin> getDataSourcePlugin(@NotNull final UUID pluginId) {
    return plugins.stream().filter(plugin -> pluginId.equals(plugin.getPluginId())).findFirst();
  }

  public List<DataSourcePlugin> getEnabledPlugins() {
    return plugins.stream().filter(DataSourcePlugin::isEnabled).collect(Collectors.toList());
  }

  DataSourcePlugin getEnabledPlugin(UUID pluginId) {

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
                return new PluginNotFoundException(
                    "No DataSourcePlugin with ID matching: " + pluginId);
              }
            });
  }

  /**
   * Set a plugin as 'enabled' (active)
   *
   * @param pluginId The ID of the plugin
   */
  public void enablePlugin(@NotNull final UUID pluginId) {

    plugins.stream()
        .filter(plugin -> pluginId.equals(plugin.getPluginId()))
        .findFirst()
        .ifPresentOrElse(
            plugin -> plugin.setEnabled(true),
            () -> {
              throw new PluginNotFoundException(
                  "Could not enable non-existent DataSourcePlugin: " + pluginId);
            });
  }

  /**
   * Disable the given plugin, so it is excluded from data refresh requests
   *
   * @param pluginId The ID of the data plugin to disable
   */
  public void disablePlugin(@NotNull final UUID pluginId) {

    plugins.stream()
        .filter(plugin -> plugin.getPluginId().equals(pluginId))
        .findFirst()
        .ifPresentOrElse(
            plugin -> plugin.setEnabled(false),
            () -> {
              throw new PluginNotFoundException(
                  "Could not disable non-existent DataSourcePlugin: " + pluginId);
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

  public void validateDataSource(@NotNull DataSource<?> dataSource) {
    final UUID pluginId = dataSource.getPluginId();
    final Optional<DataSourcePlugin> pluginOptional = getDataSourcePlugin(pluginId);
    if (pluginOptional.isPresent()) {
      final DataSourcePlugin plugin = pluginOptional.get();
      plugin.validateDataSource(dataSource);
    } else {
      throw new IllegalArgumentException(
          "DataSource invalid; no DataSourcePlugin with ID: " + pluginId);
    }
  }
}
