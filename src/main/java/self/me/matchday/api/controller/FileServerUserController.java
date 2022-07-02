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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.api.resource.FileServerUserResource;
import self.me.matchday.api.resource.FileServerUserResource.UserResourceAssembler;
import self.me.matchday.api.resource.MessageResource;
import self.me.matchday.api.service.FileServerUserService;
import self.me.matchday.model.FileServerUser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static self.me.matchday.api.resource.MessageResource.MessageResourceAssembler;

@RestController
@RequestMapping("/file-server-users")
public class FileServerUserController {

  private final FileServerUserService userService;
  private final UserResourceAssembler userResourceAssembler;
  private final MessageResourceAssembler messageResourceAssembler;

  public FileServerUserController(
      FileServerUserService userService,
      UserResourceAssembler userResourceAssembler,
      MessageResourceAssembler messageResourceAssembler) {
    this.userService = userService;
    this.userResourceAssembler = userResourceAssembler;
    this.messageResourceAssembler = messageResourceAssembler;
  }

  @RequestMapping(
      value = "/users/file-server/{id}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CollectionModel<FileServerUserResource>> getFileServerUsers(
      @PathVariable("id") final UUID pluginId) {

    final List<FileServerUser> users = userService.getAllServerUsers(pluginId);
    if (users.size() > 0) {
      return ResponseEntity.ok().body(userResourceAssembler.toCollectionModel(users));
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @RequestMapping(
      value = "/users/{userId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FileServerUserResource> getUserData(
      @PathVariable("userId") final UUID userId) {
    return userService
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
    final ClientResponse response = userService.login(user, fileServerId);
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
      @RequestParam("cookie-file") final MultipartFile cookies)
      throws IOException {

    final String cookieData = readPostTextData(cookies);
    final FileServerUser user = new FileServerUser(username, password);

    // Login via file server service
    final ClientResponse response = userService.loginWithCookies(fileServerId, user, cookieData);
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
    final ClientResponse response = userService.logout(user, fileServerId);
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
    final ClientResponse response = userService.relogin(fileServerUser, fileServerId);
    // Extract message
    final String responseText = getResponseMessage(response);
    final MessageResource messageResource = messageResourceAssembler.toModel(responseText);

    return ResponseEntity.status(response.statusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(messageResource);
  }

  // === Helpers ===
  /**
   * Extract body message from a client response
   *
   * @param response The ClientResponse from the file server
   * @return The response body as a String (not null)
   */
  private @NotNull String getResponseMessage(@NotNull ClientResponse response) {
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
  private String readPostTextData(final @NotNull MultipartFile file) throws IOException {

    String result;
    try (InputStream is = file.getInputStream();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      result = reader.lines().collect(Collectors.joining("\n"));
    }
    return result;
  }
}
