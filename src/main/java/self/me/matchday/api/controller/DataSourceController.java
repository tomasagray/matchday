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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import self.me.matchday.api.resource.DataSourcePluginResource;
import self.me.matchday.api.resource.DataSourcePluginResource.DataSourcePluginResourceAssembler;
import self.me.matchday.api.resource.MessageResource;
import self.me.matchday.api.service.DataSourceService;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Event;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.DataSourcePlugin;
import self.me.matchday.util.Log;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping(value = "/data-sources")
public class DataSourceController {

  private static final String LOG_TAG = "DataSourceController";

  private final DataSourceService dataSourceService;
  private final MessageResource.MessageResourceAssembler messageResourceAssembler;
  private final DataSourcePluginResourceAssembler pluginResourceAssembler;

  DataSourceController(
      DataSourceService dataSourceService,
      DataSourcePluginResourceAssembler pluginResourceAssembler,
      MessageResource.MessageResourceAssembler messageResourceAssembler) {

    this.dataSourceService = dataSourceService;
    this.pluginResourceAssembler = pluginResourceAssembler;
    this.messageResourceAssembler = messageResourceAssembler;
  }

  @RequestMapping(
      value = "/refresh/all",
      method = {RequestMethod.POST, RequestMethod.GET},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SnapshotRequest> refreshAllSources(
      @RequestBody SnapshotRequest snapshotRequest) {

    Log.i(
        LOG_TAG,
        String.format("Refreshing all data sources with SnapshotRequest: %s\n\n", snapshotRequest));

    final SnapshotRequest status = dataSourceService.refreshDataSources(snapshotRequest);
    return ResponseEntity.ok().body(status);
  }

  @RequestMapping(
      value = {"", "/"},
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CollectionModel<DataSourcePluginResource> getAllPlugins() {

    // Retrieve all plugins from service
    final Set<DataSourcePlugin<Event>> dataSourcePlugins = dataSourceService.getDataSourcePlugins();
    return pluginResourceAssembler.toCollectionModel(dataSourcePlugins);
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

    HttpStatus status;
    String message;
    final boolean enabled = dataSourceService.enablePlugin(pluginId);

    if (enabled) {
      status = HttpStatus.ACCEPTED;
      message = String.format("Plugin with ID: %s successfully enabled", pluginId);
    } else {
      status = HttpStatus.BAD_REQUEST;
      message = String.format("Plugin with ID %s could not be enabled", pluginId);
    }
    return ResponseEntity.status(status).body(messageResourceAssembler.toModel(message));
  }

  @RequestMapping(
      value = "/plugin/{pluginId}/disable",
      method = {RequestMethod.POST, RequestMethod.GET},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MessageResource> disablePlugin(
      @PathVariable("pluginId") final UUID pluginId) {

    HttpStatus status;
    String message;
    final boolean disabled = dataSourceService.disablePlugin(pluginId);

    if (disabled) {
      status = HttpStatus.OK;
      message = String.format("Successfully disabled plugin with ID: %s", pluginId);
    } else {
      status = HttpStatus.BAD_REQUEST;
      message = String.format("Could not disable plugin with ID: %s", pluginId);
    }
    return ResponseEntity.status(status).body(messageResourceAssembler.toModel(message));
  }

  @RequestMapping(
      value = "/add-data-source",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MessageResource> addDataSource(@RequestBody DataSource dataSource) {

    final DataSource source = dataSourceService.addDataSource(dataSource);
    final MessageResource messageResource =
        messageResourceAssembler.toModel("Successfully added new DataSource: " + source.getId());
    return ResponseEntity.ok(messageResource);
  }

  @RequestMapping(
      value = "/get-data-source/{dataSourceId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<DataSource> getDataSource(@PathVariable("dataSourceId") Long dataSourceId) {

    return dataSourceService
        .getDataSourceById(dataSourceId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
