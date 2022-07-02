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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import self.me.matchday.api.resource.FileServerResource;
import self.me.matchday.api.resource.FileServerResource.FileServerResourceAssembler;
import self.me.matchday.api.resource.MessageResource;
import self.me.matchday.api.resource.MessageResource.MessageResourceAssembler;
import self.me.matchday.api.service.FileServerPluginService;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping(value = "/file-servers")
public class FileServerPluginController {

  private static final Logger logger = LogManager.getLogger(FileServerPluginController.class);

  private final FileServerPluginService fileServerPluginService;
  private final FileServerResourceAssembler serverResourceAssembler;
  private final MessageResourceAssembler messageResourceAssembler;

  public FileServerPluginController(
      FileServerPluginService fileServerPluginService,
      FileServerResourceAssembler serverResourceAssembler,
      MessageResourceAssembler messageResourceAssembler) {

    this.fileServerPluginService = fileServerPluginService;
    this.serverResourceAssembler = serverResourceAssembler;
    this.messageResourceAssembler = messageResourceAssembler;
  }

  @RequestMapping(
      value = "/all",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CollectionModel<FileServerResource> getAllFileServerPlugins() {
    return serverResourceAssembler.toCollectionModel(
        fileServerPluginService.getFileServerPlugins());
  }

  @RequestMapping(
      value = "/all/enabled",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CollectionModel<FileServerResource> getEnabledFileServers() {
    return serverResourceAssembler.toCollectionModel(fileServerPluginService.getEnabledPlugins());
  }

  @RequestMapping(
      value = "/file-server/{id}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FileServerResource> getFileServerById(
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
  public ResponseEntity<MessageResource> enableFileServerPlugin(
      @PathVariable("id") final UUID pluginId) {

    ResponseEntity<MessageResource> response;
    if (fileServerPluginService.enablePlugin(pluginId)) {
      response =
          ResponseEntity.status(HttpStatus.OK)
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  messageResourceAssembler.toModel(
                      "Enabled file server plugin with ID: " + pluginId));
    } else {
      response =
          ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  messageResourceAssembler.toModel(
                      "Could not enable file server plugin with ID: " + pluginId));
    }
    return response;
  }

  @RequestMapping(
      value = "/file-server/{id}/disable",
      method = {RequestMethod.POST, RequestMethod.GET},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody ResponseEntity<MessageResource> disableFileServerPlugin(
      @PathVariable("id") final UUID pluginId) {

    ResponseEntity<MessageResource> response;
    if (fileServerPluginService.disablePlugin(pluginId)) {
      response =
          ResponseEntity.status(HttpStatus.OK)
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  messageResourceAssembler.toModel(
                      "Successfully disabled plugin with ID: " + pluginId));
    } else {
      response =
          ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  messageResourceAssembler.toModel(
                      "Could not disable plugin with ID: " + pluginId));
    }
    return response;
  }

  @ExceptionHandler(IOException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<String> handleIoException(@NotNull IOException e) {
    String message = e.getMessage();
    final Throwable cause = e.getCause();
    logger.error(
        "Could not read text from multi-part POST data: {} with root cause: {}",
        message,
        cause.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
  }
}
