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

package self.me.matchday.api.controller;

import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import self.me.matchday.api.resource.DataSourcePluginResource;
import self.me.matchday.api.resource.DataSourcePluginResource.DataSourcePluginResourceAssembler;
import self.me.matchday.api.resource.DataSourceResource;
import self.me.matchday.api.resource.MessageResource;
import self.me.matchday.api.service.DataSourceService;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.SnapshotRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static self.me.matchday.api.resource.DataSourceResource.DataSourceResourceAssembler;

@RestController
@RequestMapping(value = "/data-sources")
public class DataSourceController {

  private final DataSourceService dataSourceService;
  private final MessageResource.MessageResourceAssembler messageResourceAssembler;
  private final DataSourceResourceAssembler dataSourceResourceAssembler;
  private final DataSourcePluginResourceAssembler pluginResourceAssembler;

  DataSourceController(
      DataSourceService dataSourceService,
      DataSourcePluginResourceAssembler pluginResourceAssembler,
      DataSourceResourceAssembler dataSourceResourceAssembler,
      MessageResource.MessageResourceAssembler messageResourceAssembler) {

    this.dataSourceService = dataSourceService;
    this.pluginResourceAssembler = pluginResourceAssembler;
    this.dataSourceResourceAssembler = dataSourceResourceAssembler;
    this.messageResourceAssembler = messageResourceAssembler;
  }

  @RequestMapping(
      value = "/refresh/all",
      method = {RequestMethod.POST, RequestMethod.GET},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SnapshotRequest> refreshAllSources(
      @RequestBody SnapshotRequest snapshotRequest) throws IOException {

    final SnapshotRequest status = dataSourceService.refreshAllDataSources(snapshotRequest);
    return ResponseEntity.ok().body(status);
  }

  @RequestMapping(
      value = {"/plugin/all"},
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CollectionModel<DataSourcePluginResource> getAllPlugins() {
    return pluginResourceAssembler.toCollectionModel(dataSourceService.getDataSourcePlugins());
  }

  @RequestMapping(
      value = "/plugin/{pluginId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<DataSourcePluginResource> getDataSourcePlugin(
      @PathVariable("pluginId") final UUID pluginId) {

    return dataSourceService
        .getDataSourcePlugin(pluginId)
        .map(pluginResourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(
      value = "/plugin/{pluginId}/enable",
      method = {RequestMethod.POST, RequestMethod.GET},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MessageResource> enablePlugin(
      @PathVariable("pluginId") final UUID pluginId) {

    try {
      dataSourceService.enablePlugin(pluginId);
      final String message = String.format("Plugin with ID: %s successfully enabled", pluginId);
      return ResponseEntity.ok().body(messageResourceAssembler.toModel(message));
    } catch (Throwable e) {
      final String msg =
          String.format(
              "Plugin with ID %s could not be enabled; reason: %s", pluginId, e.getMessage());
      return ResponseEntity.badRequest().body(messageResourceAssembler.toModel(msg));
    }
  }

  @RequestMapping(
      value = "/plugin/{pluginId}/disable",
      method = {RequestMethod.POST, RequestMethod.GET},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MessageResource> disablePlugin(
      @PathVariable("pluginId") final UUID pluginId) {

    try {
      dataSourceService.disablePlugin(pluginId);
      final String message = String.format("Plugin with ID: %s successfully disabled", pluginId);
      return ResponseEntity.ok().body(messageResourceAssembler.toModel(message));
    } catch (Throwable e) {
      final String msg =
          String.format(
              "Plugin with ID %s could not be disabled; reason: %s", pluginId, e.getMessage());
      return ResponseEntity.badRequest().body(messageResourceAssembler.toModel(msg));
    }
  }

  @RequestMapping(
      value = "/add-data-source",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MessageResource> addDataSource(@RequestBody DataSource<?> dataSource) {

    final DataSource<?> source = dataSourceService.addDataSource(dataSource);
    final MessageResource messageResource =
        messageResourceAssembler.toModel(
            "Successfully added new DataSource: " + source.getPluginId());
    return ResponseEntity.ok(messageResource);
  }

  @RequestMapping(
      value = "/plugin/{pluginId}/sources",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CollectionModel<DataSourceResource>> getDataSourcesForPlugin(
      @PathVariable("pluginId") UUID pluginId) {

    List<DataSource<?>> dataSources = dataSourceService.getDataSourcesForPlugin(pluginId);
    return ResponseEntity.ok(dataSourceResourceAssembler.toCollectionModel(dataSources));
  }

  @RequestMapping(
      value = "/get-data-source/{dataSourceId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<DataSourceResource> getDataSource(
      @PathVariable("dataSourceId") UUID dataSourceId) {

    return dataSourceService
        .getDataSourceById(dataSourceId)
        .map(dataSourceResourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
