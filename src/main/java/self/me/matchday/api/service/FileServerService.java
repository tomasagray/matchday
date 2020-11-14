/*
 * Copyright (c) 2020.
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

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.db.FileServerUserRepo;
import self.me.matchday.model.SecureCookie;
import self.me.matchday.plugin.fileserver.FileServerPlugin;
import self.me.matchday.plugin.fileserver.FileServerUser;
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

  @Getter
  private final List<FileServerPlugin> fileServerPlugins;
  private final FileServerUserRepo userRepo;
  private final SecureDataService secureDataService;

  @Autowired
  FileServerService(@NotNull final List<FileServerPlugin> fileServerPlugins,
      @NotNull final FileServerUserRepo userRepo,
      @NotNull final SecureDataService secureDataService) {

    this.fileServerPlugins = fileServerPlugins;
    this.userRepo = userRepo;
    this.secureDataService = secureDataService;
  }

  /**
   * Get the required fileserver plugin by its ID
   *
   * @param plugId The ID of the fileserver plugin
   * @return The requested fileserver plugin, or null if not found
   */
  public Optional<FileServerPlugin> getPluginById(@Nullable final UUID plugId) {

    final FileServerPlugin serverPlugin =
        fileServerPlugins
            .stream()
            .collect(Collectors.toMap(FileServerPlugin::getPluginId, plugin -> plugin))
            .get(plugId);

    return
        (serverPlugin != null) ?
            Optional.of(serverPlugin) :
            Optional.empty();
  }

  // === Login ===

  /**
   * Log a file server user (FileServerUser) into the appropriate file server.
   *
   * @param fileServerUser   The User.
   * @param pluginId The pluginId of the fileserver plugin
   * @return Was login successful? (true/false)
   */
  public ClientResponse login(@NotNull final FileServerUser fileServerUser, @NotNull final UUID pluginId) {

    // Result container
    ClientResponse response;

    // Find required plugin
    final Optional<FileServerPlugin> pluginOptional = getPluginById(pluginId);
    if (pluginOptional.isPresent()) {

      final FileServerPlugin fileServerPlugin = pluginOptional.get();

      Log.i(LOG_TAG, "Found plugin with ID: " + pluginId);
      response = fileServerPlugin.login(fileServerUser);

      // If login successful
      if (response.statusCode().is2xxSuccessful()) {

        final String serverId = fileServerPlugin.getPluginId().toString();
        // Extract cookies
        final List<SecureCookie> cookies =
            response
                .cookies()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .map(SecureCookie::fromSpringCookie)
                .collect(Collectors.toList());

        // Login user
        fileServerUser.loginToServer(serverId, cookies);
        // Save user to repo
        userRepo.save(fileServerUser);
        Log.i(LOG_TAG, "Login SUCCESSFUL with user: " + fileServerUser);
      }
    } else {
      // Plugin not found; bad request
      response =
          ClientResponse
              .create(HttpStatus.BAD_REQUEST)
              .body(String.format("Invalid plugin ID: %s", pluginId))
              .build();
    }

    // Return login response
    return response;
  }

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
        user.setLoggedOut();
        // Save data
        userRepo.saveAndFlush(user);

        Log.i(LOG_TAG, String.format("Logged out user: %s", userData));

        return
            ClientResponse
                .create(HttpStatus.OK)
                .body(String.format("User %s successfully logged out", userData.getUserName()))
                .build();
      } else {
        failureMessage.add("Invalid server ID");
      }
    } else {
      failureMessage.add(String.format("User with ID: %s not found", user.getUserId()));
    }

    // Logout failed
    return
        ClientResponse
            .create(HttpStatus.BAD_REQUEST)
            .body(failureMessage.toString())
            .build();
  }

  public ClientResponse relogin(@NotNull final FileServerUser fileServerUser, @NotNull final UUID pluginId) {

    // Get complete user data
    final Optional<FileServerUser> userOptional = userRepo.findById(fileServerUser.getUserId());
    if (userOptional.isPresent() && fileServerUser.equals(userOptional.get())) {
      Log.i(LOG_TAG, String.format("Re-logging in user: %s", fileServerUser));
      return login(fileServerUser, pluginId);
    }

    return
        ClientResponse
            .create(HttpStatus.BAD_REQUEST)
            .body(String.format("Relogin failed: User %s not found", fileServerUser.getUserName()))
            .build();
  }

  // === Users ===

  /**
   * Get all users associated with a particular fileserver plugin
   *
   * @param pluginId ID of the fileserver plugin
   * @return A List of FSUsers
   */
  public Optional<List<FileServerUser>> getAllServerUsers(@NotNull final UUID pluginId) {

    return
        userRepo.fetchAllUsersForServer(pluginId.toString());
  }

  /**
   * Retrieve a specific user from the database by ID
   *
   * @param userId The ID of the user
   * @return An Optional containing the user, if found
   */
  public Optional<FileServerUser> getUserById(@NotNull final String userId) {

    return
        userRepo.findById(userId);
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
    final FileServerPlugin pluginForUrl = getPluginForUrl(externalUrl);
    if (pluginForUrl != null) {
      // Get a logged in user
      final FileServerUser downloadUser = getDownloadUser(pluginForUrl.getPluginId());

      if (downloadUser != null) {
        // Decrypt user cookies
        final List<HttpCookie> httpCookies =
            downloadUser.getCookies().stream()
                .map(secureDataService::decryptData)
                .map(SecureCookie::toSpringCookie)
                .collect(Collectors.toList());
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
    final FileServerPlugin fileServerPlugin = getPluginForUrl(url);
    // Return the recommended refresh rate for this FS manager
    return
        (fileServerPlugin != null) ?
            fileServerPlugin.getRefreshRate() :
            DEFAULT_REFRESH_RATE;
  }

  /**
   * Find the first registered file server manager which can decode the given URL.
   *
   * @param url The external URL
   * @return The first registered fileserver manager which can handle the URL.
   */
  private @Nullable FileServerPlugin getPluginForUrl(@NotNull final URL url) {

    for (final FileServerPlugin plugin : this.fileServerPlugins) {
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
    final Optional<List<FileServerUser>> userOptional =
        userRepo.fetchLoggedInUsersForServer(pluginId.toString());
    if (userOptional.isPresent()) {
      final List<FileServerUser> users = userOptional.get();
      // Return the download user
      return
          users.get(0);
    }
    // No logged in users for this plugin
    return null;
  }
}
