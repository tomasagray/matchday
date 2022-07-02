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

package self.me.matchday.api.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.db.FileServerUserRepo;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.model.SecureCookie;
import self.me.matchday.plugin.fileserver.FileServerPlugin;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileServerUserService {

  private final FileServerUserRepo userRepo;
  private final UserValidationService userValidationService;
  private final NetscapeCookiesService cookiesService;
  private final FileServerPluginService pluginService;

  public FileServerUserService(
      FileServerUserRepo userRepo,
      UserValidationService userValidationService,
      NetscapeCookiesService cookiesService,
      FileServerPluginService pluginService) {
    this.userRepo = userRepo;
    this.userValidationService = userValidationService;
    this.cookiesService = cookiesService;
    this.pluginService = pluginService;
  }

  // === Login ===

  /**
   * Log a file server user (FileServerUser) into the appropriate file server.
   *
   * @param user The User.
   * @param pluginId The pluginId of the file server plugin
   * @return Was login successful? (true/false)
   */
  @Transactional
  public ClientResponse login(@NotNull final FileServerUser user, @NotNull final UUID pluginId) {

    // TODO: refactor this method, use immutable login object
    // Result container
    HttpStatus status = HttpStatus.BAD_REQUEST;
    String message;

    if (userValidationService.isValidUserData(user)) {
      // Validate login data
      final Optional<FileServerPlugin> pluginOptional = pluginService.getPluginById(pluginId);
      if (pluginOptional.isPresent()) {
        final FileServerPlugin serverPlugin = pluginOptional.get();
        final ClientResponse loginResponse = serverPlugin.login(user);
        // If login successful
        if (loginResponse.statusCode().is2xxSuccessful()) {
          // Extract cookies
          final List<SecureCookie> cookies =
              loginResponse.cookies().values().stream()
                  .flatMap(Collection::stream)
                  .map(SecureCookie::fromSpringCookie)
                  .collect(Collectors.toList());
          // Login user to server
          user.setLoggedIntoServer(serverPlugin.getPluginId(), cookies);
          // Save user to repo
          final FileServerUser flushedUser = userRepo.saveAndFlush(user);
          // Return successful response
          status = HttpStatus.OK;
          message =
              String.format(
                  "User: %s successfully logged into file server: %s",
                  flushedUser.getUsername(), serverPlugin.getTitle());
        } else {
          return loginResponse;
        }
      } else {
        // Plugin not found; bad request
        message = String.format("File server plugin not found: %s", pluginId);
      }
    } else {
      message = "Invalid user data passed: " + user;
    }
    // Return login response
    return ClientResponse.create(status).body(message).build();
  }

  /**
   * Login a user to a file server using previously gathered cookies
   *
   * @param pluginId The ID of the file server
   * @param user The user to login
   * @param cookieData A collection of cookies necessary to access secure parts of the server
   * @return The response
   */
  @Transactional
  public ClientResponse loginWithCookies(
      @NotNull final UUID pluginId,
      @NotNull final FileServerUser user,
      @NotNull final String cookieData) {

    // Result containers
    HttpStatus status = HttpStatus.BAD_REQUEST;
    String message;

    // Parse cookies
    final List<SecureCookie> cookies =
        cookiesService.parseNetscapeCookies(cookieData).stream()
            .map(SecureCookie::fromSpringCookie)
            .collect(Collectors.toList());

    // Validate user data
    if (userValidationService.isValidUserData(user)) {
      // Validate cookies
      if (!cookies.isEmpty()) {
        // Validate plugin ID
        final Optional<FileServerPlugin> pluginOptional = pluginService.getPluginById(pluginId);
        if (pluginOptional.isPresent()) {
          final FileServerPlugin serverPlugin = pluginOptional.get();
          // Login user to appropriate server
          user.setLoggedIntoServer(serverPlugin.getPluginId(), cookies);
          // Save to repo
          userRepo.save(user);
          // Return successful response
          status = HttpStatus.OK;
          message =
              String.format(
                  "User: %s successfully logged into file server: %s",
                  user.getUsername(), serverPlugin.getTitle());
        } else {
          message = String.format("File server ID: %s NOT FOUND", pluginId);
        }
      } else {
        message = "Not cookies supplied with login request";
      }
    } else {
      message = "Invalid user data for user: " + user.getUsername();
    }
    return ClientResponse.create(status).body(message).build();
  }

  /**
   * Log a user out of a given file server
   *
   * @param user The user to be logged out
   * @param pluginId The ID of the plugin for the file server
   * @return The response from the file server upon logging out
   */
  @Transactional
  public ClientResponse logout(@NotNull final FileServerUser user, @NotNull final UUID pluginId) {

    final StringJoiner failureMessage = new StringJoiner(" ");
    failureMessage.add("User logout failed:");

    // Find required user
    final Optional<FileServerUser> userOptional = userRepo.findById(user.getUserId());
    if (userOptional.isPresent()) {
      final FileServerUser userData = userOptional.get();
      // Validate server ID
      if (userData.getServerId().equals(pluginId)) {
        // Perform logout request
        userData.setLoggedOut();
        // Save data
        userRepo.saveAndFlush(userData);
        return ClientResponse.create(HttpStatus.OK)
            .body(String.format("User %s successfully logged out", userData.getUsername()))
            .build();
      } else {
        failureMessage.add("Invalid server ID");
      }
    } else {
      failureMessage.add(String.format("User with ID: %s not found", user.getUserId()));
    }
    return ClientResponse.create(HttpStatus.BAD_REQUEST).body(failureMessage.toString()).build();
  }

  /**
   * Re-login a user which is already registered with the specified plugin.
   *
   * @param user The user to be re-logged in
   * @param pluginId The ID of the file server plugin
   * @return The login response
   */
  @Transactional
  public ClientResponse relogin(@NotNull final FileServerUser user, @NotNull final UUID pluginId) {

    String message;
    // Get complete user data
    final Optional<FileServerUser> userOptional = userRepo.findById(user.getUserId());
    if (userOptional.isPresent()) {
      final FileServerUser fileServerUser = userOptional.get();
      if (!fileServerUser.isLoggedIn()) {
        return login(fileServerUser, pluginId);
      } else {
        message = String.format("User: %s already logged in", user.getUsername());
      }
    } else {
      message = String.format("Relogin failed: User %s not found", user.getUsername());
    }
    return ClientResponse.create(HttpStatus.BAD_REQUEST).body(message).build();
  }

  // === Users ===

  /**
   * Get all users associated with a particular file server plugin
   *
   * @param pluginId ID of the file server plugin
   * @return A List of FSUsers
   */
  public List<FileServerUser> getAllServerUsers(@NotNull final UUID pluginId) {
    return userRepo.fetchAllUsersForServer(pluginId);
  }

  /**
   * Retrieve a specific user from the database by ID
   *
   * @param userId The ID of the user
   * @return An Optional containing the user, if found
   */
  public Optional<FileServerUser> getUserById(@NotNull final UUID userId) {
    return userRepo.findById(userId);
  }

  /**
   * Delete a user from the database
   *
   * @param userId The ID of the user to delete
   */
  @Transactional
  public void deleteUser(@NotNull final UUID userId) {

    // Ensure user is in DB
    final Optional<FileServerUser> userOptional = userRepo.findById(userId);
    if (userOptional.isPresent()) {
      final FileServerUser fileServerUser = userOptional.get();
      userRepo.delete(fileServerUser);
    }
  }
}
