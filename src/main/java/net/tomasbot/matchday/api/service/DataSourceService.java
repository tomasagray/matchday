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

package net.tomasbot.matchday.api.service;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.tomasbot.matchday.db.DataSourceRepository;
import net.tomasbot.matchday.db.PatternKitRepository;
import net.tomasbot.matchday.model.*;
import net.tomasbot.matchday.plugin.datasource.DataSourcePlugin;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class DataSourceService implements EntityService<DataSource<?>, UUID> {

  private static final int MAX_LABELS = 25;
  private static final int MAX_ALLOWABLE_RESULTS = 10_000;

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

  private static SnapshotRequest validateSnapshotRequest(@NotNull SnapshotRequest request) {
    LocalDateTime startDate = request.getStartDate();
    LocalDateTime endDate = request.getEndDate();
    if (startDate != null && endDate != null && startDate.isAfter(endDate))
      throw new IllegalArgumentException("Start date is after end date");

    List<String> labels = request.getLabels();
    if (labels != null && labels.size() > MAX_LABELS)
      throw new IllegalArgumentException("Too many labels");

    int maxResults = request.getMaxResults();
    if (maxResults < 0 || maxResults > MAX_ALLOWABLE_RESULTS)
      throw new IllegalArgumentException("Illegal max results: " + maxResults);

    // returned sanitized version
    return SnapshotRequest.from(request);
  }

  /**
   * Refresh all <b>enabled</b> data sources with the given Snapshot
   *
   * @param request Refresh request details
   * @return The SnapshotRequest, for additional processing
   */
  public SnapshotRequest refreshAllDataSources(@NotNull SnapshotRequest request)
      throws IOException {
    SnapshotRequest cleaned = validateSnapshotRequest(request);

    List<DataSourcePlugin> enabledPlugins = pluginService.getEnabledPlugins();
    if (enabledPlugins.isEmpty())
      throw new SystemConfigException("No data source plugins are enabled");

    for (DataSourcePlugin plugin : enabledPlugins) {
      refreshDataSourcesForPlugin(cleaned, plugin);
    }

    return cleaned;
  }

  private void refreshDataSourcesForPlugin(
      @NotNull SnapshotRequest request, @NotNull DataSourcePlugin plugin) throws IOException {
    UUID pluginId = plugin.getPluginId();
    List<DataSource<?>> dataSources =
        dataSourceRepository.findDataSourcesByPluginId(pluginId).stream()
            .filter(DataSource::isEnabled)
            .toList();
    if (dataSources.isEmpty())
      throw new SystemConfigException("No data sources are enabled for plugin: " + pluginId);

    for (DataSource<?> dataSource : dataSources) {
      refreshDataSource(request, dataSource);
    }
  }

  private <T> void refreshDataSource(
      @NotNull SnapshotRequest request, @NotNull DataSource<T> dataSource) throws IOException {
    DataSourcePlugin dataSourcePlugin = pluginService.getEnabledPlugin(dataSource.getPluginId());
    Snapshot<T> snapshot = dataSourcePlugin.getSnapshot(request, dataSource);

    if (snapshot != null) snapshotService.saveSnapshot(snapshot, dataSource.getClazz());
    else
      throw new IllegalArgumentException(
          "No snapshot returned for DataSource: " + dataSource.getDataSourceId());
  }

  @SuppressWarnings("unchecked cast")
  public <T> void refreshOnUrl(@NotNull URL url) throws IOException {
    Optional<DataSource<?>> dataSourceOptional = findDataSourceForUrl(url);
    if (dataSourceOptional.isPresent()) {
      DataSource<T> dataSource = (DataSource<T>) dataSourceOptional.get();
      if (!dataSource.isEnabled())
        throw new IllegalArgumentException("Data source is disabled: " + dataSource.getPluginId());

      DataSourcePlugin plugin = pluginService.getEnabledPlugin(dataSource.getPluginId());
      Snapshot<T> snapshot = plugin.getUrlSnapshot(url, dataSource);
      snapshotService.saveSnapshot(snapshot, dataSource.getClazz());
    } else throw new IllegalArgumentException("No matching DataSource for URL: " + url);
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
    DataSource<?> saved = dataSourceRepository.save(dataSource);
    if (dataSource instanceof final PlaintextDataSource<?> plaintext)
      patternKitRepository.saveAll(plaintext.getPatternKits());

    return saved;
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
    UUID dataSourceId = dataSource.getDataSourceId();
    Optional<DataSource<?>> sourceOptional = fetchById(dataSourceId);
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

  public boolean toggleDataSourceEnabled(@NotNull UUID dataSourceId, boolean isEnabled) {
    Optional<DataSource<?>> sourceOptional = fetchById(dataSourceId);
    if (sourceOptional.isPresent()) {
      DataSource<?> dataSource = sourceOptional.get();
      dataSource.setEnabled(isEnabled);
      DataSource<?> saved = dataSourceRepository.saveAndFlush(dataSource);

      return saved.isEnabled();
    } else throw new IllegalArgumentException("Data source not found: " + dataSourceId);
  }

  public boolean toggleIsDataSourceFlared(@NotNull UUID dataSourceId, boolean isFlared) {
    Optional<DataSource<?>> sourceOptional = fetchById(dataSourceId);
    if (sourceOptional.isPresent()) {
      DataSource<?> dataSource = sourceOptional.get();
      dataSource.setFlared(isFlared);
      DataSource<?> saved = dataSourceRepository.saveAndFlush(dataSource);

      return saved.isFlared();
    } else throw new IllegalArgumentException("Data source not found: " + dataSourceId);
  }
}
