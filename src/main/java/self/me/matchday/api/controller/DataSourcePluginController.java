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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.DataSourcePluginResource;
import self.me.matchday.api.resource.DataSourcePluginResource.DataSourcePluginResourceAssembler;
import self.me.matchday.api.resource.MessageResource;
import self.me.matchday.api.service.DataSourcePluginService;

import java.util.UUID;

@RestController
@RequestMapping("/data-source-plugins")
public class DataSourcePluginController {

  private final DataSourcePluginService dataSourcePluginService;
  private final DataSourcePluginResourceAssembler pluginResourceAssembler;
  private final MessageResource.MessageResourceAssembler messageResourceAssembler;

  DataSourcePluginController(
      DataSourcePluginService dataSourcePluginService,
      DataSourcePluginResourceAssembler pluginResourceAssembler,
      MessageResource.MessageResourceAssembler messageResourceAssembler) {
    this.dataSourcePluginService = dataSourcePluginService;
    this.pluginResourceAssembler = pluginResourceAssembler;
    this.messageResourceAssembler = messageResourceAssembler;
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
  public ResponseEntity<MessageResource> enablePlugin(
      @PathVariable("pluginId") final UUID pluginId) {

    try {
      dataSourcePluginService.enablePlugin(pluginId);
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
      dataSourcePluginService.disablePlugin(pluginId);
      final String message = String.format("Plugin with ID: %s successfully disabled", pluginId);
      return ResponseEntity.ok().body(messageResourceAssembler.toModel(message));
    } catch (Throwable e) {
      final String msg =
          String.format(
              "Plugin with ID %s could not be disabled; reason: %s", pluginId, e.getMessage());
      return ResponseEntity.badRequest().body(messageResourceAssembler.toModel(msg));
    }
  }
}
