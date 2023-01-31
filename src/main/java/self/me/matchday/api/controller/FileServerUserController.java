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
import org.springframework.web.multipart.MultipartFile;
import self.me.matchday.api.resource.FileServerUserResource;
import self.me.matchday.api.resource.FileServerUserResource.UserResourceAssembler;
import self.me.matchday.api.service.FileServerLoginException;
import self.me.matchday.api.service.FileServerUserService;
import self.me.matchday.api.service.InvalidCookieException;
import self.me.matchday.model.FileServerUser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/file-server-users")
public class FileServerUserController {

  private final FileServerUserService userService;
  private final UserResourceAssembler userResourceAssembler;

  public FileServerUserController(
      FileServerUserService userService, UserResourceAssembler userResourceAssembler) {
    this.userService = userService;
    this.userResourceAssembler = userResourceAssembler;
  }

  @RequestMapping(
      value = "/users/file-server/{id}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CollectionModel<FileServerUserResource>> getFileServerUsers(
      @PathVariable("id") final UUID pluginId) {

    final List<FileServerUser> users = userService.getAllServerUsers(pluginId);
    return ResponseEntity.ok().body(userResourceAssembler.toCollectionModel(users));
  }

  @RequestMapping(
      value = "/user/{userId}",
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
      value = "/login",
      method = {RequestMethod.POST, RequestMethod.GET},
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FileServerUserResource> loginToFileServer(
      @RequestBody final FileServerUser user) {

    final FileServerUser loggedInUser = userService.login(user);
    return ResponseEntity.ok(userResourceAssembler.toModel(loggedInUser));
  }

  @RequestMapping(
      value = "/user/login-with-cookies",
      method = {RequestMethod.GET, RequestMethod.POST},
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FileServerUserResource> loginWithCookies(
      @RequestParam("username") final String username,
      @RequestParam("serverId") final UUID pluginId,
      @RequestParam("cookies") final MultipartFile cookieData)
      throws IOException {

    final String cookies = readPostTextData(cookieData);
    final FileServerUser user = new FileServerUser(username, "", pluginId);
    final FileServerUser loggedInUser = userService.loginWithCookies(user, cookies);
    return ResponseEntity.ok(userResourceAssembler.toModel(loggedInUser));
  }

  @RequestMapping(
      value = "/user/{userId}/logout",
      method = {RequestMethod.POST, RequestMethod.GET},
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FileServerUserResource> logoutOfFileServer(
      @PathVariable("userId") UUID userId) {

    final FileServerUser user = userService.logout(userId);
    return ResponseEntity.ok(userResourceAssembler.toModel(user));
  }

  @RequestMapping(
      value = "/user/{userId}/relogin",
      method = {RequestMethod.POST, RequestMethod.GET},
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FileServerUserResource> reloginToFileServer(
      @PathVariable("userId") final UUID userId) {

    final FileServerUser user = userService.relogin(userId);
    return ResponseEntity.ok(userResourceAssembler.toModel(user));
  }

  @RequestMapping(
      value = "/user/{userId}/delete",
      method = RequestMethod.DELETE,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UUID> deleteFileServerUser(@PathVariable("userId") UUID userId) {
    userService.deleteUser(userId);
    return ResponseEntity.ok(userId);
  }

  @ExceptionHandler({
    IOException.class,
    InvalidCookieException.class
  })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public String handleIoException(@NotNull Exception e) {
    return e.getMessage();
  }

  @ExceptionHandler(FileServerLoginException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  @ResponseBody
  public String handleLoginDenied(@NotNull Exception e) {
    return "Access denied: " + e.getMessage();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public String handleUserNotFound(@NotNull Exception e) {
    return e.getMessage();
  }

  /**
   * Read text data from the POST input buffer
   *
   * @param file The file pointer for the text data
   * @return A String (may be null)
   */
  private String readPostTextData(final @NotNull MultipartFile file) throws IOException {

    try (InputStream is = file.getInputStream();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      return reader.lines().collect(Collectors.joining("\n"));
    }
  }
}
