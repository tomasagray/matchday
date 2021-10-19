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

package self.me.matchday.api.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.db.FileServerUserRepo;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.model.SecureCookie;
import self.me.matchday.plugin.fileserver.FileServerPlugin;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class to route requests for URL parsing (external -> internal decoding) to the appropriate File
 * Server Manager.
 */
@Service
public class FileServerService {

  private static final String LOG_TAG = "FileServerService";
  private static final Duration DEFAULT_REFRESH_RATE = Duration.ofHours(4);

  private final List<FileServerPlugin> fileServerPlugins;
  private final List<FileServerPlugin> enabledPlugins =
      Collections.synchronizedList(new ArrayList<>());
  private final FileServerUserRepo userRepo;
  private final SecureDataService secureDataService;
  private final UserValidationService userValidationService;
  private final NetscapeCookiesService cookiesService;

  @Autowired
  FileServerService(
      final List<FileServerPlugin> fileServerPlugins,
      final FileServerUserRepo userRepo,
      final SecureDataService secureDataService,
      final UserValidationService userValidationService,
      final NetscapeCookiesService cookiesService) {

    this.fileServerPlugins = fileServerPlugins;
    // Default: all plugins enabled
    this.enabledPlugins.addAll(fileServerPlugins);
    this.userRepo = userRepo;
    this.secureDataService = secureDataService;
    this.userValidationService = userValidationService;
    this.cookiesService = cookiesService;
  }

  // === Plugin management ===

  /**
   * Get all registered file server plugins
   *
   * @return a List<> of file server plugins
   */
  public List<FileServerPlugin> getFileServerPlugins() {
    return this.fileServerPlugins;
  }

  public List<FileServerPlugin> getEnabledPlugins() {
    return this.enabledPlugins;
  }

  /**
   * Determine if a given plugin is currently active
   *
   * @param pluginId ID of the plugin
   * @return True/false Is the plugin currently active?
   */
  public boolean isPluginEnabled(@NotNull final UUID pluginId) {

    boolean enabledContains = false;
    // determine if any plugin in the enabled list matches given ID
    for (FileServerPlugin plugin : enabledPlugins) {
      if (pluginId.equals(plugin.getPluginId())) {
        enabledContains = true;
        break;
      }
    }
    return enabledContains;
  }

  /**
   * Get the required fileserver plugin by its ID
   *
   * @param plugId The ID of the fileserver plugin
   * @return The requested fileserver plugin, or null if not found
   */
  public Optional<FileServerPlugin> getPluginById(@Nullable final UUID plugId) {

    final FileServerPlugin serverPlugin =
        fileServerPlugins.stream()
            .collect(Collectors.toMap(FileServerPlugin::getPluginId, plugin -> plugin))
            .get(plugId);

    return (serverPlugin != null) ? Optional.of(serverPlugin) : Optional.empty();
  }

  /**
   * Disable a specific file server plugin
   *
   * @param pluginId The ID of the plugin
   * @return True/false - was the plugin disabled
   */
  public boolean disablePlugin(@NotNull final UUID pluginId) {

    final Optional<FileServerPlugin> pluginOptional = getPluginById(pluginId);
    if (pluginOptional.isPresent()) {
      final FileServerPlugin fileServerPlugin = pluginOptional.get();
      // Remove from enabled plugins list
      return enabledPlugins.remove(fileServerPlugin);
    } else {
      Log.i(LOG_TAG, "Requested to disable non-existent plugin: " + pluginId);
      return false;
    }
  }

  /**
   * Enable a previously disabled file server plugin
   *
   * @param pluginId The ID of the plugin to enable
   * @return True/false - if the plugin was successfully enabled
   */
  public boolean enablePlugin(@NotNull final UUID pluginId) {

    // Retrieve requested plugin
    final Optional<FileServerPlugin> pluginOptional = getPluginById(pluginId);
    if (pluginOptional.isPresent()) {
      final FileServerPlugin fileServerPlugin = pluginOptional.get();
      // Add to enabled plugins
      return enabledPlugins.add(fileServerPlugin);
    } else {
      Log.i(LOG_TAG, "Requested to enable non-existent plugin: " + pluginId);
      return false;
    }
  }

  // === Login ===

  /**
   * Log a file server user (FileServerUser) into the appropriate file server.
   *
   * @param user The User.
   * @param pluginId The pluginId of the fileserver plugin
   * @return Was login successful? (true/false)
   */
  public ClientResponse login(@NotNull final FileServerUser user, @NotNull final UUID pluginId) {

    // Result container
    HttpStatus status = HttpStatus.BAD_REQUEST;
    String message;

    if (userValidationService.isValidUserData(user)) {
      // Validate login data
      final Optional<FileServerPlugin> pluginOptional = getPluginById(pluginId);
      if (pluginOptional.isPresent()) {
        final FileServerPlugin serverPlugin = pluginOptional.get();
        Log.i(LOG_TAG, "Found plugin with ID: " + pluginId);
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
          user.setLoggedIntoServer(serverPlugin.getPluginId().toString(), cookies);
          // Save user to repo
          userRepo.save(user);
          Log.i(LOG_TAG, "Login SUCCESSFUL with user: " + user);
          // Return successful response
          status = HttpStatus.OK;
          message =
              String.format(
                  "User: %s successfully logged into file server: %s",
                  user.getUsername(), serverPlugin.getTitle());
        } else {
          Log.i(
              LOG_TAG,
              String.format(
                  "Login request was DENIED for user: %s @ server: %s",
                  user.getUsername(), serverPlugin.getTitle()));
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
  public ClientResponse loginWithCookies(
      @NotNull final UUID pluginId,
      @NotNull final FileServerUser user,
      @NotNull final String cookieData) {

    // Result containers
    HttpStatus status = HttpStatus.BAD_REQUEST;
    String message;
    Log.i(LOG_TAG, String.format("Attempting login with user: %s, plugin: %s", user, pluginId));

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
        final Optional<FileServerPlugin> pluginOptional = getPluginById(pluginId);
        if (pluginOptional.isPresent()) {
          final FileServerPlugin serverPlugin = pluginOptional.get();
          // Login user to appropriate server
          user.setLoggedIntoServer(serverPlugin.getPluginId().toString(), cookies);
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
    Log.i(LOG_TAG, message);
    return ClientResponse.create(status).body(message).build();
  }

  /**
   * Log a user out of a given file server
   *
   * @param user The user to be logged out
   * @param pluginId The ID of the plugin for the file server
   * @return The response from the file server upon logging out
   */
  public ClientResponse logout(@NotNull final FileServerUser user, @NotNull final UUID pluginId) {

    final StringJoiner failureMessage = new StringJoiner(" ");
    failureMessage.add("User logout failed:");

    // Find required user
    final Optional<FileServerUser> userOptional = userRepo.findById(user.getUserId());
    if (userOptional.isPresent()) {
      final FileServerUser userData = userOptional.get();
      // Validate server ID
      if (userData.getServerId().equals(pluginId.toString())) {
        // Perform logout request
        userData.setLoggedOut();
        // Save data
        userRepo.saveAndFlush(userData);

        Log.i(LOG_TAG, String.format("Logged out user: %s", userData));

        return ClientResponse.create(HttpStatus.OK)
            .body(String.format("User %s successfully logged out", userData.getUsername()))
            .build();
      } else {
        failureMessage.add("Invalid server ID");
      }
    } else {
      failureMessage.add(String.format("User with ID: %s not found", user.getUserId()));
    }

    // Logout failed
    return ClientResponse.create(HttpStatus.BAD_REQUEST).body(failureMessage.toString()).build();
  }

  /**
   * Re-login a user which is already registered with the specified plugin.
   *
   * @param user The user to be re-logged in
   * @param pluginId The ID of the file server plugin
   * @return The login response
   */
  public ClientResponse relogin(@NotNull final FileServerUser user, @NotNull final UUID pluginId) {

    String message;

    // Get complete user data
    final Optional<FileServerUser> userOptional = userRepo.findById(user.getUserId());
    if (userOptional.isPresent()) {
      final FileServerUser fileServerUser = userOptional.get();
      if (!fileServerUser.isLoggedIn()) {

        Log.i(LOG_TAG, String.format("Re-logging in user: %s", fileServerUser));
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
   * Get all users associated with a particular fileserver plugin
   *
   * @param pluginId ID of the fileserver plugin
   * @return A List of FSUsers
   */
  public List<FileServerUser> getAllServerUsers(@NotNull final UUID pluginId) {
    return userRepo.fetchAllUsersForServer(pluginId.toString());
  }

  /**
   * Retrieve a specific user from the database by ID
   *
   * @param userId The ID of the user
   * @return An Optional containing the user, if found
   */
  public Optional<FileServerUser> getUserById(@NotNull final String userId) {

    return userRepo.findById(userId);
  }

  /**
   * Delete a user from the database
   *
   * @param userId The ID of the user to delete
   */
  public void deleteUser(@NotNull final String userId) {

    // Ensure user is in DB
    final Optional<FileServerUser> userOptional = userRepo.findById(userId);
    if (userOptional.isPresent()) {

      final FileServerUser fileServerUser = userOptional.get();
      Log.i(LOG_TAG, "Deleting user: " + fileServerUser);
      userRepo.delete(fileServerUser);

    } else {
      Log.i(LOG_TAG, String.format("User: %s not found", userId));
    }
  }

  // === Downloads ===

  /**
   * Wraps the getDownloadUrl() method of each File Server, and routes the request to the
   * appropriate one.
   *
   * @param externalUrl The external-facing (public) URL
   * @return The internal (private) URL.
   */
  public Optional<URL> getDownloadUrl(@NotNull final URL externalUrl) throws IOException {

    // Get correct FS manager
    final FileServerPlugin pluginForUrl = getEnabledPluginForUrl(externalUrl);
    if (pluginForUrl != null) {
      // Get a logged in user
      final FileServerUser downloadUser = getDownloadUser(pluginForUrl.getPluginId());
      if (downloadUser != null) {
        // Decrypt user cookies
        final Set<HttpCookie> httpCookies =
            downloadUser.getCookies().stream()
                .map(secureDataService::decryptData)
                .map(SecureCookie::toSpringCookie)
                .collect(Collectors.toSet());
        // Use the FS plugin to get the internal (download) URL
        return pluginForUrl.getDownloadURL(externalUrl, httpCookies);
      } else {
        throw new IOException("No logged in user could download requested URL: " + externalUrl);
      }
    } else {
      throw new IOException("Could not find plugin matching URL: " + externalUrl);
    }
  }

  /**
   * Returns the recommended refresh rate for data associated with the given URL. If this cannot be
   * determined from the URL, returns a default value instead.
   *
   * @param url The external URL for the fileserver
   * @return The recommended refresh rate.
   */
  public Duration getFileServerRefreshRate(@NotNull final URL url) {

    // Get the fileserver manager for this URL
    final FileServerPlugin fileServerPlugin = getEnabledPluginForUrl(url);
    // Return the recommended refresh rate for this FS manager
    return (fileServerPlugin != null) ? fileServerPlugin.getRefreshRate() : DEFAULT_REFRESH_RATE;
  }

  /**
   * Find the first registered file server manager which can decode the given URL.
   *
   * @param url The external URL
   * @return The first registered fileserver manager which can handle the URL.
   */
  public @Nullable FileServerPlugin getEnabledPluginForUrl(@NotNull final URL url) {
    // search only ENABLED plugins
    for (final FileServerPlugin plugin : this.enabledPlugins) {
      if (plugin.acceptsUrl(url)) {
        return plugin;
      }
    }
    // No suitable plugin found
    return null;
  }

  /**
   * Find a suitable user to user for download page translation.
   *
   * @param pluginId The ID of the file server plugin
   * @return A logged-in fileserver user, or null if none found
   */
  private @Nullable FileServerUser getDownloadUser(@NotNull final UUID pluginId) {
    // Get logged-in users for this repo
    final List<FileServerUser> users = userRepo.fetchLoggedInUsersForServer(pluginId.toString());
    if (users.size() > 0) {
      // Return the download user
      return users.get(0);
    }
    // No logged-in users for this plugin
    return null;
  }
}
