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
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Service;
import self.me.matchday.db.FileServerUserRepo;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.model.SecureCookie;
import self.me.matchday.plugin.fileserver.FileServerPlugin;

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
public class FileServerPluginService {

  private static final Duration DEFAULT_REFRESH_RATE = Duration.ofHours(4);

  private final List<FileServerPlugin> fileServerPlugins;
  private final List<FileServerPlugin> enabledPlugins =
      Collections.synchronizedList(new ArrayList<>()); // todo - remove this, use only 1 collection
  private final FileServerUserRepo userRepo;
  private final SecureDataService secureDataService;

  FileServerPluginService(
      List<FileServerPlugin> fileServerPlugins,
      FileServerUserRepo userRepo,
      SecureDataService secureDataService) {

    this.fileServerPlugins = fileServerPlugins;
    // Default: all plugins enabled
    this.enabledPlugins.addAll(fileServerPlugins);
    this.userRepo = userRepo;
    this.secureDataService = secureDataService;
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
   * Get the required file server plugin by its ID
   *
   * @param plugId The ID of the file server plugin
   * @return The requested file server plugin, or null if not found
   */
  public Optional<FileServerPlugin> getPluginById(@Nullable final UUID plugId) {

    final FileServerPlugin serverPlugin =
        fileServerPlugins.stream()
            .collect(Collectors.toMap(FileServerPlugin::getPluginId, plugin -> plugin))
            .get(plugId);

    return (serverPlugin != null) ? Optional.of(serverPlugin) : Optional.empty();
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
      return false;
    }
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
      return false;
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
   * @param url The external URL for the file server
   * @return The recommended refresh rate.
   */
  public Duration getFileServerRefreshRate(@NotNull final URL url) {

    // Get the file server manager for this URL
    final FileServerPlugin fileServerPlugin = getEnabledPluginForUrl(url);
    // Return the recommended refresh rate for this FS manager
    return (fileServerPlugin != null) ? fileServerPlugin.getRefreshRate() : DEFAULT_REFRESH_RATE;
  }

  /**
   * Find the first registered file server manager which can decode the given URL.
   *
   * @param url The external URL
   * @return The first registered file server manager which can handle the URL.
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
   * @return A logged-in file server user, or null if none found
   */
  private @Nullable FileServerUser getDownloadUser(@NotNull final UUID pluginId) {
    // Get logged-in users for this repo
    final List<FileServerUser> users = userRepo.fetchLoggedInUsersForServer(pluginId);
    if (users.size() > 0) {
      // Return the download user
      return users.get(0);
    }
    // No logged-in users for this plugin
    return null;
  }
}
