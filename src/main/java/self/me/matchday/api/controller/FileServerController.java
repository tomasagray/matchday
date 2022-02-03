/*
 * Copyright (c) 2021.
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.api.resource.FileServerResource;
import self.me.matchday.api.resource.FileServerResource.FileServerResourceAssembler;
import self.me.matchday.api.resource.FileServerUserResource;
import self.me.matchday.api.resource.FileServerUserResource.UserResourceAssembler;
import self.me.matchday.api.resource.MessageResource;
import self.me.matchday.api.resource.MessageResource.MessageResourceAssembler;
import self.me.matchday.api.service.FileServerService;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.plugin.fileserver.FileServerPlugin;
import self.me.matchday.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/file-servers")
public class FileServerController {

  private static final String LOG_TAG = "FileServerController";

  private final FileServerService fileServerService;
  private final FileServerResourceAssembler serverResourceAssembler;
  private final UserResourceAssembler userResourceAssembler;
  private final MessageResourceAssembler messageResourceAssembler;

  @Autowired
  public FileServerController(
      @NotNull final FileServerService fileServerService,
      @NotNull final FileServerResourceAssembler serverResourceAssembler,
      @NotNull final UserResourceAssembler userResourceAssembler,
      @NotNull final MessageResourceAssembler messageResourceAssembler) {

    this.fileServerService = fileServerService;
    this.serverResourceAssembler = serverResourceAssembler;
    this.userResourceAssembler = userResourceAssembler;
    this.messageResourceAssembler = messageResourceAssembler;
  }

  // === GET ===
  @RequestMapping(value = "/all", method = RequestMethod.GET)
  public CollectionModel<FileServerResource> getAllFileServers() {

    final Collection<FileServerPlugin> fileServerPlugins = fileServerService.getFileServerPlugins();
    return serverResourceAssembler.toCollectionModel(fileServerPlugins);
  }

  @RequestMapping(value = "/file-server/{id}", method = RequestMethod.GET)
  public ResponseEntity<FileServerResource> getFileServerById(
      @PathVariable("id") final UUID pluginId) {

    return fileServerService
        .getPluginById(pluginId)
        .map(serverResourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(value = "/file-server/{id}/users", method = RequestMethod.GET)
  public ResponseEntity<CollectionModel<FileServerUserResource>> getFileServerUsers(
      @PathVariable("id") final UUID pluginId) {

    final List<FileServerUser> users = fileServerService.getAllServerUsers(pluginId);
    if (users.size() > 0) {
      return ResponseEntity.ok().body(userResourceAssembler.toCollectionModel(users));
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @RequestMapping(value = "/users/{userId}", method = RequestMethod.GET)
  public ResponseEntity<FileServerUserResource> getUserData(
      @PathVariable("userId") final UUID userId) {
    return fileServerService
        .getUserById(userId)
        .map(userResourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  // === Login ===
  @RequestMapping(
      value = "/file-server/{id}/login",
      method = {RequestMethod.POST, RequestMethod.GET},
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MessageResource> loginToFileServer(
      @RequestBody final FileServerUser user, @PathVariable("id") final UUID fileServerId) {

    // Login to correct file server & parse response
    final ClientResponse response = fileServerService.login(user, fileServerId);
    final String messageText = getResponseMessage(response);
    final MessageResource messageResource = messageResourceAssembler.toModel(messageText);

    // Send response to end user
    return ResponseEntity.status(response.statusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(messageResource);
  }

  @RequestMapping(
      value = "/file-server/{id}/login-with-cookies",
      method = {RequestMethod.POST, RequestMethod.GET},
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<MessageResource> loginWithCookies(
      @PathVariable("id") final UUID fileServerId,
      @RequestParam("username") final String username,
      @RequestParam("password") final String password,
      @RequestParam("cookie-file") final MultipartFile cookies) {

    final String cookieData = readPostTextData(cookies);
    final FileServerUser user = new FileServerUser(username, password);

    // Login via file server service
    final ClientResponse response =
        fileServerService.loginWithCookies(fileServerId, user, cookieData);
    final String messageText = getResponseMessage(response);
    final MessageResource message = messageResourceAssembler.toModel(messageText);

    // Return response
    return ResponseEntity.status(response.statusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(message);
  }

  @RequestMapping(
      value = "/file-server/{id}/logout",
      method = {RequestMethod.POST, RequestMethod.GET},
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody ResponseEntity<MessageResource> logoutOfFileServer(
      @RequestBody final FileServerUser user, @PathVariable("id") final UUID fileServerId) {

    // Perform logout request
    final ClientResponse response = fileServerService.logout(user, fileServerId);
    // Extract response message
    final String responseText = getResponseMessage(response);
    final MessageResource messageResource = messageResourceAssembler.toModel(responseText);

    return ResponseEntity.status(response.statusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(messageResource);
  }

  @RequestMapping(
      value = "/file-server/{id}/relogin",
      method = {RequestMethod.POST, RequestMethod.GET},
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody ResponseEntity<MessageResource> reloginToFileServer(
      @RequestBody final FileServerUser fileServerUser,
      @PathVariable("id") final UUID fileServerId) {

    // Perform login request
    final ClientResponse response = fileServerService.relogin(fileServerUser, fileServerId);
    // Extract message
    final String responseText = getResponseMessage(response);
    final MessageResource messageResource = messageResourceAssembler.toModel(responseText);

    return ResponseEntity.status(response.statusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(messageResource);
  }

  // === Plugin management ===
  @RequestMapping(
      value = "/file-server/{id}/disable",
      method = {RequestMethod.POST, RequestMethod.GET},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody ResponseEntity<MessageResource> disableFileServerPlugin(
      @PathVariable("id") final UUID pluginId) {

    ResponseEntity<MessageResource> response;

    Log.i(LOG_TAG, "Attempting to disable file server plugin: " + pluginId);
    if (fileServerService.disablePlugin(pluginId)) {
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

  @RequestMapping(
      value = "/file-server/{id}/enable",
      method = {RequestMethod.POST, RequestMethod.GET},
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MessageResource> enableFileServerPlugin(
      @PathVariable("id") final UUID pluginId) {

    ResponseEntity<MessageResource> response;

    Log.i(LOG_TAG, "Attempting to enable file server plugin with ID: " + pluginId);
    if (fileServerService.enablePlugin(pluginId)) {
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

  // === Helpers ===
  /**
   * Extract body message from a client response
   *
   * @param response The ClientResponse from the file server
   * @return The response body as a String (not null)
   */
  private @NotNull String getResponseMessage(ClientResponse response) {
    // Extract response message
    final String responseText = response.bodyToMono(String.class).block();
    // Ensure response message is not null & return
    return (responseText != null) ? responseText : "";
  }

  /**
   * Read text data from the POST input buffer
   *
   * @param file The file pointer for the text data
   * @return A String (may be null)
   */
  private String readPostTextData(final MultipartFile file) {

    // Result container
    String result = null;

    try (InputStream is = file.getInputStream();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

      // Read POST data
      result = reader.lines().collect(Collectors.joining("\n"));

    } catch (IOException e) {
      Log.i(LOG_TAG, "Could not read text from multi-part POST data");
    }

    return result;
  }
}
