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

package net.tomasbot.matchday.api.service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import net.tomasbot.matchday.db.FileServerUserRepo;
import net.tomasbot.matchday.model.FileServerUser;
import net.tomasbot.matchday.model.SecureCookie;
import net.tomasbot.matchday.plugin.fileserver.FileServerPlugin;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ClientResponse;

@Service
public class FileServerUserService {

  private final FileServerUserRepo userRepo;
  private final UserValidationService userValidationService;
  private final CookiesService cookiesService;
  private final FileServerPluginService pluginService;

  public FileServerUserService(
      FileServerUserRepo userRepo,
      UserValidationService userValidationService,
      CookiesService cookiesService,
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
   * @return Was login successful? (true/false)
   */
  @Transactional
  public FileServerUser login(@NotNull final FileServerUser user) {
    userValidationService.validateUserForLogin(user);

    final UUID pluginId = user.getServerId();
    final Optional<FileServerPlugin> pluginOptional = pluginService.getPluginById(pluginId);
    if (pluginOptional.isPresent()) {
      final FileServerPlugin serverPlugin = pluginOptional.get();
      final ClientResponse loginResponse = serverPlugin.login(user);

      if (loginResponse.statusCode().is2xxSuccessful()) {
        final List<SecureCookie> cookies = readResponseCookies(loginResponse);
        return setUserLoggedInToServer(user, serverPlugin.getPluginId(), cookies);
      } else {
        final String message = loginResponse.bodyToMono(String.class).block();
        throw new FileServerLoginException(message);
      }
    } else {
      throw new PluginNotFoundException(
          "Attempting to login to non-existent file server: " + pluginId);
    }
  }

  @NotNull
  private List<SecureCookie> readResponseCookies(@NotNull ClientResponse loginResponse) {
    return loginResponse.cookies().values().stream()
        .flatMap(Collection::stream)
        .map(SecureCookie::fromSpringCookie)
        .collect(Collectors.toList());
  }

  /**
   * Login a user to a file server using previously gathered cookieData. Overwrites any cookieData
   * which may be present in the FileServerUser instance.
   *
   * @param user The user to login
   * @param cookieData A String representing a cookies file
   * @return The response
   */
  @Transactional
  public FileServerUser loginWithCookies(
      @NotNull final FileServerUser user, @NotNull final String cookieData) {
    userValidationService.validateEmailAddress(user.getUsername());
    final UUID pluginId = user.getServerId();
    final Optional<FileServerPlugin> pluginOptional = pluginService.getPluginById(pluginId);
    if (pluginOptional.isPresent()) {
      final FileServerPlugin serverPlugin = pluginOptional.get();
      final Collection<SecureCookie> cookies = parseCookieData(cookieData);
      cookiesService.validateCookies(cookies);
      // Login user to appropriate server
      return setUserLoggedInToServer(user, serverPlugin.getPluginId(), cookies);
    } else {
      throw new PluginNotFoundException("No file server found with ID: " + pluginId);
    }
  }

  private @NotNull FileServerUser setUserLoggedInToServer(
      @NotNull FileServerUser user,
      @NotNull UUID serverId,
      @NotNull Collection<SecureCookie> cookies) {
    final Optional<FileServerUser> loggedInOptional = userRepo.findByUsername(user.getUsername());
    if (loggedInOptional.isPresent()) {
      final FileServerUser loggedInUser = loggedInOptional.get();
      loggedInUser.setLoggedIntoServer(serverId, cookies);
      return loggedInUser;
    } else {
      user.setLoggedIntoServer(serverId, cookies);
      return userRepo.saveAndFlush(user);
    }
  }

  @NotNull
  private List<SecureCookie> parseCookieData(@NotNull String cookieData) {
    return cookiesService.parseCookies(cookieData).stream()
        .map(SecureCookie::fromSpringCookie)
        .collect(Collectors.toList());
  }

  /**
   * Log a user out of a given file server
   *
   * @param userId The ID of user to be logged out
   * @return The updated User
   * @throws IllegalArgumentException if user not found
   */
  @Transactional
  public FileServerUser logout(@NotNull final UUID userId) {
    // Find required user
    final Optional<FileServerUser> userOptional = userRepo.findById(userId);
    if (userOptional.isPresent()) {
      final FileServerUser user = userOptional.get();
      // Perform logout request
      user.setLoggedOut();
      user.setCookies(new ArrayList<>());
      // Save data
      return userRepo.saveAndFlush(user);
    } else {
      throw new IllegalArgumentException(String.format("User with ID: %s not found", userId));
    }
  }

  /**
   * Re-login a user which is already registered with the specified plugin.
   *
   * @param userId The ID user to be re-logged in
   * @return The logged-in user
   */
  @Transactional
  public FileServerUser relogin(@NotNull final UUID userId) {
    final Optional<FileServerUser> userOptional = userRepo.findById(userId);
    if (userOptional.isPresent()) {
      final FileServerUser fileServerUser = userOptional.get();
      return login(fileServerUser);
    } else {
      throw new IllegalArgumentException(
          String.format("Relogin failed: User %s not found", userId));
    }
  }

  // === Users ===

  public List<FileServerUser> getAllUsers() {
    return userRepo.findAll();
  }

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
    final Optional<FileServerUser> userOptional = userRepo.findById(userId);
    if (userOptional.isPresent()) {
      final FileServerUser fileServerUser = userOptional.get();
      userRepo.delete(fileServerUser);
    } else {
      throw new IllegalArgumentException("Trying to DELETE non-existent user: " + userId);
    }
  }

  public float getRemainingBandwidthFor(UUID userId) throws IOException {
    Optional<FileServerUser> userOptional = getUserById(userId);
    if (userOptional.isPresent()) {
      FileServerUser user = userOptional.get();
      UUID serverId = user.getServerId();
      Optional<FileServerPlugin> pluginOptional = pluginService.getPluginById(serverId);

      if (pluginOptional.isPresent()) {
        FileServerPlugin plugin = pluginOptional.get();
        Set<HttpCookie> cookies = pluginService.getHttpCookies(user);
        return plugin.getRemainingBandwidth(cookies);
      }

      throw new IllegalArgumentException("Could not locate file server plugin: " + serverId);
    }

    throw new IllegalArgumentException("No such file server user: " + userId);
  }
}
