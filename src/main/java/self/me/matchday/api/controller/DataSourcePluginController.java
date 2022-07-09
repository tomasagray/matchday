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

import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import self.me.matchday.api.resource.DataSourcePluginResource;
import self.me.matchday.api.resource.DataSourcePluginResource.DataSourcePluginResourceAssembler;
import self.me.matchday.api.service.DataSourcePluginService;

import java.util.UUID;

@RestController
@RequestMapping("/data-source-plugins")
public class DataSourcePluginController {

  private final DataSourcePluginService dataSourcePluginService;
  private final DataSourcePluginResourceAssembler pluginResourceAssembler;

  DataSourcePluginController(
      DataSourcePluginService dataSourcePluginService,
      DataSourcePluginResourceAssembler pluginResourceAssembler) {
    this.dataSourcePluginService = dataSourcePluginService;
    this.pluginResourceAssembler = pluginResourceAssembler;
  }

  @RequestMapping(
      value = {"/all"},
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CollectionModel<DataSourcePluginResource> getAllPlugins() {
    return pluginResourceAssembler.toCollectionModel(
        dataSourcePluginService.getDataSourcePlugins());
  }

  @RequestMapping(
      value = "/plugin/{pluginId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<DataSourcePluginResource> getDataSourcePlugin(
      @PathVariable("pluginId") final UUID pluginId) {

    return dataSourcePluginService
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
  public @ResponseBody UUID enablePlugin(@PathVariable("pluginId") final UUID pluginId) {
    dataSourcePluginService.enablePlugin(pluginId);
    return pluginId;
  }

  @RequestMapping(
      value = "/plugin/{pluginId}/disable",
      method = {RequestMethod.POST, RequestMethod.GET},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody UUID disablePlugin(@PathVariable("pluginId") final UUID pluginId) {
    dataSourcePluginService.disablePlugin(pluginId);
    return pluginId;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public String handleIllegalArgumentException(@NotNull IllegalArgumentException e) {
    return e.getMessage();
  }
}
