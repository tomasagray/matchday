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

package net.tomasbot.matchday.api.controller;

import static net.tomasbot.matchday.api.resource.DataSourceResource.DataSourceResourceAssembler;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import net.tomasbot.matchday.api.resource.DataSourceResource;
import net.tomasbot.matchday.api.service.DataSourceService;
import net.tomasbot.matchday.model.DataSource;
import net.tomasbot.matchday.model.PlaintextDataSource;
import net.tomasbot.matchday.model.SnapshotRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/data-sources")
public class DataSourceController {

  private final DataSourceService dataSourceService;
  private final DataSourceResourceAssembler dataSourceResourceAssembler;

  DataSourceController(
      DataSourceService dataSourceService,
      DataSourceResourceAssembler dataSourceResourceAssembler) {

    this.dataSourceService = dataSourceService;
    this.dataSourceResourceAssembler = dataSourceResourceAssembler;
  }

  @RequestMapping(
      value = "/refresh/on-url",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public void refreshOnUrl(@RequestBody @NotNull UrlRequest request) throws IOException {
    dataSourceService.refreshOnUrl(request.getUrl());
  }

  @RequestMapping(
      value = "/all",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CollectionModel<DataSourceResource>> fetchAllDataSources() {
    final List<DataSource<?>> dataSources = dataSourceService.fetchAll();
    return ResponseEntity.ok(dataSourceResourceAssembler.toCollectionModel(dataSources));
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
  public ResponseEntity<DataSource<?>> addPlaintextDataSource(
      @RequestBody PlaintextDataSource<?> dataSource) {
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

  @RequestMapping(
      value = "/data-source/{dataSourceId}/update",
      method = RequestMethod.PATCH,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<DataSourceResource> updatePlaintextDataSource(
      @RequestBody PlaintextDataSource<?> dataSource) {

    final DataSource<?> updatedDataSource = dataSourceService.update(dataSource);
    return ResponseEntity.ok(dataSourceResourceAssembler.toModel(updatedDataSource));
  }

  @RequestMapping(value = "/data-source/{dataSourceId}/delete", method = RequestMethod.DELETE)
  public ResponseEntity<UUID> deleteDataSource(@PathVariable("dataSourceId") UUID dataSourceID) {
    dataSourceService.delete(dataSourceID);
    return ResponseEntity.ok(dataSourceID);
  }

  @Data
  public static class UrlRequest {
    private URL url;
  }
}
