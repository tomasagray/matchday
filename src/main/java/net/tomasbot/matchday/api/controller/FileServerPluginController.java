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

import java.io.IOException;
import java.util.UUID;
import net.tomasbot.matchday.api.resource.FileServerPluginResource;
import net.tomasbot.matchday.api.resource.FileServerPluginResource.FileServerResourceAssembler;
import net.tomasbot.matchday.api.service.FileServerPluginService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/file-servers")
public class FileServerPluginController {

  private static final Logger logger = LogManager.getLogger(FileServerPluginController.class);

  private final FileServerPluginService fileServerPluginService;
  private final FileServerResourceAssembler serverResourceAssembler;

  public FileServerPluginController(
      FileServerPluginService fileServerPluginService,
      FileServerResourceAssembler serverResourceAssembler) {

    this.fileServerPluginService = fileServerPluginService;
    this.serverResourceAssembler = serverResourceAssembler;
  }

  @RequestMapping(
      value = "/all",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CollectionModel<FileServerPluginResource> getAllFileServerPlugins() {
    return serverResourceAssembler.toCollectionModel(
        fileServerPluginService.getFileServerPlugins());
  }

  @RequestMapping(
      value = "/all/enabled",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CollectionModel<FileServerPluginResource> getEnabledFileServers() {
    return serverResourceAssembler.toCollectionModel(fileServerPluginService.getEnabledPlugins());
  }

  @RequestMapping(
      value = "/file-server/{id}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FileServerPluginResource> getFileServerById(
      @PathVariable("id") final UUID pluginId) {

    return fileServerPluginService
        .getPluginById(pluginId)
        .map(serverResourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(
      value = "/file-server/{id}/enable",
      method = {RequestMethod.POST, RequestMethod.GET},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody UUID enableFileServerPlugin(@PathVariable("id") final UUID pluginId) {
    fileServerPluginService.enablePlugin(pluginId);
    return pluginId;
  }

  @RequestMapping(
      value = "/file-server/{id}/disable",
      method = {RequestMethod.POST, RequestMethod.GET},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody UUID disableFileServerPlugin(@PathVariable("id") final UUID pluginId) {
    fileServerPluginService.disablePlugin(pluginId);
    return pluginId;
  }

  @ExceptionHandler(IOException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public String handleIoException(@NotNull IOException e) {
    String message = e.getMessage();
    final Throwable cause = e.getCause();
    logger.error(
        "Could not read text from multi-part POST data: {} with root cause: {}",
        message,
        cause.getMessage());
    return message;
  }
}
