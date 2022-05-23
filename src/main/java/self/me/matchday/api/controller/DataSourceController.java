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
import self.me.matchday.api.resource.DataSourcePluginResource.DataSourcePluginResourceAssembler;
import self.me.matchday.api.resource.DataSourceResource;
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
  private final DataSourceResourceAssembler dataSourceResourceAssembler;

  DataSourceController(
      DataSourceService dataSourceService,
      DataSourcePluginResourceAssembler pluginResourceAssembler,
      DataSourceResourceAssembler dataSourceResourceAssembler) {

    this.dataSourceService = dataSourceService;
    this.dataSourceResourceAssembler = dataSourceResourceAssembler;
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
      value = "/data-source/add",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<DataSource<?>> addDataSource(@RequestBody DataSource<?> dataSource) {
    final DataSource<?> source = dataSourceService.save(dataSource);
    return ResponseEntity.ok(source);
  }

  @RequestMapping(
      value = "/plugin/{pluginId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CollectionModel<DataSourceResource>> getDataSourcesForPlugin(
      @PathVariable("pluginId") UUID pluginId) {

    List<DataSource<?>> dataSources = dataSourceService.getDataSourcesForPlugin(pluginId);
    return ResponseEntity.ok(dataSourceResourceAssembler.toCollectionModel(dataSources));
  }

  @RequestMapping(
      value = "/data-source/{dataSourceId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<DataSourceResource> getDataSource(
      @PathVariable("dataSourceId") UUID dataSourceId) {

    return dataSourceService
        .fetchById(dataSourceId)
        .map(dataSourceResourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
