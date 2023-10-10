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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.DataSourceRepository;
import self.me.matchday.db.PatternKitRepository;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.PlaintextDataSource;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.DataSourcePlugin;

@Service
@Transactional
public class DataSourceService implements EntityService<DataSource<?>, UUID> {

  private final SnapshotService snapshotService;
  private final DataSourcePluginService pluginService;
  private final DataSourceRepository dataSourceRepository;
  private final PatternKitRepository patternKitRepository;

  DataSourceService(
      SnapshotService snapshotService,
      DataSourceRepository dataSourceRepository,
      DataSourcePluginService pluginService,
      PatternKitRepository patternKitRepository) {
    this.snapshotService = snapshotService;
    this.dataSourceRepository = dataSourceRepository;
    this.pluginService = pluginService;
    this.patternKitRepository = patternKitRepository;
  }

  /**
   * Refresh all <b>enabled</b> data sources with the given Snapshot
   *
   * @param request Refresh request details
   * @return The SnapshotRequest, for additional processing
   */
  public SnapshotRequest refreshAllDataSources(@NotNull final SnapshotRequest request)
      throws IOException {

    for (DataSourcePlugin plugin : pluginService.getEnabledPlugins()) {
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

    final DataSourcePlugin dataSourcePlugin =
        pluginService.getEnabledPlugin(dataSource.getPluginId());
    final Snapshot<T> snapshot = dataSourcePlugin.getSnapshot(request, dataSource);
    snapshotService.saveSnapshot(snapshot, dataSource.getClazz());
  }

  @SuppressWarnings("unchecked cast")
  public <T> void refreshOnUrl(@NotNull URL url) throws IOException {

    Optional<DataSource<?>> dataSourceOptional = findDataSourceForUrl(url);
    if (dataSourceOptional.isPresent()) {
      DataSource<T> dataSource = (DataSource<T>) dataSourceOptional.get();
      DataSourcePlugin plugin = pluginService.getEnabledPlugin(dataSource.getPluginId());
      Snapshot<T> snapshot = plugin.getUrlSnapshot(url, dataSource);
      snapshotService.saveSnapshot(snapshot, dataSource.getClazz());
    } else {
      throw new IllegalArgumentException("No matching DataSource for URL: " + url);
    }
  }

  private Optional<DataSource<?>> findDataSourceForUrl(@NotNull URL url) {
    List<DataSource<?>> dataSources = dataSourceRepository.findAll();
    for (DataSource<?> dataSource : dataSources) {
      String sourceHost = dataSource.getBaseUri().toString();
      if (url.toString().startsWith(sourceHost)) {
        return Optional.of(dataSource);
      }
    }
    return Optional.empty();
  }

  @Override
  public DataSource<?> initialize(@NotNull DataSource<?> dataSource) {
    Hibernate.initialize(dataSource);
    return dataSource;
  }

  @Override
  public DataSource<?> save(@NotNull final DataSource<?> dataSource) {
    pluginService.validateDataSource(dataSource);
    if (dataSource instanceof final PlaintextDataSource<?> plaintext) {
      patternKitRepository.saveAll(plaintext.getPatternKits());
    }
    return dataSourceRepository.save(dataSource);
  }

  @Override
  public List<DataSource<?>> saveAll(@NotNull Iterable<? extends DataSource<?>> entities) {
    return StreamSupport.stream(entities.spliterator(), false)
        .map(this::save)
        .collect(Collectors.toList());
  }

  public List<DataSource<?>> getDataSourcesForPlugin(@NotNull UUID pluginId) {
    return dataSourceRepository.findDataSourcesByPluginId(pluginId);
  }

  @Override
  public Optional<DataSource<?>> fetchById(@NotNull UUID id) {
    return dataSourceRepository.findById(id);
  }

  @Override
  public List<DataSource<?>> fetchAll() {
    return dataSourceRepository.findAll();
  }

  @Override
  public DataSource<?> update(@NotNull final DataSource<?> dataSource) {
    final UUID dataSourceId = dataSource.getDataSourceId();
    final Optional<DataSource<?>> sourceOptional = dataSourceRepository.findById(dataSourceId);
    if (sourceOptional.isPresent()) {
      pluginService.validateDataSource(dataSource);
      return dataSourceRepository.saveAndFlush(dataSource);
    }
    // else...
    throw new IllegalArgumentException(
        "Attempting to update nonexistent DataSource with ID: " + dataSourceId);
  }

  @Override
  public List<DataSource<?>> updateAll(@NotNull Iterable<? extends DataSource<?>> entities) {
    return StreamSupport.stream(entities.spliterator(), false)
        .map(this::update)
        .collect(Collectors.toList());
  }

  @Override
  public void delete(@NotNull UUID dataSourceId) {
    dataSourceRepository.deleteById(dataSourceId);
  }

  @Override
  public void deleteAll(@NotNull Iterable<? extends DataSource<?>> entities) {
    dataSourceRepository.deleteAll(entities);
  }
}
