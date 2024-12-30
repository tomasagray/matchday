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

import static net.tomasbot.matchday.config.settings.EnabledFileServerPlugins.ENABLED_FILESERVERS;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import net.tomasbot.matchday.db.FileServerUserRepo;
import net.tomasbot.matchday.model.FileServerUser;
import net.tomasbot.matchday.model.SecureCookie;
import net.tomasbot.matchday.plugin.fileserver.FileServerPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Service;

/**
 * Class to route requests for URL parsing (external -> internal decoding) to the appropriate File
 * Server Manager.
 */
@Service
public class FileServerPluginService {

  private static final Duration DEFAULT_REFRESH_RATE = Duration.ofHours(4);

  @Getter private final List<FileServerPlugin> fileServerPlugins;
  private final FileServerUserRepo userRepo;
  private final SecureDataService secureDataService;
  private final PluginService pluginService;

  FileServerPluginService(
      List<FileServerPlugin> fileServerPlugins,
      FileServerUserRepo userRepo,
      SecureDataService secureDataService,
      PluginService pluginService) {
    this.fileServerPlugins = fileServerPlugins;
    this.userRepo = userRepo;
    this.secureDataService = secureDataService;
    this.pluginService = pluginService;
  }

  // === Plugin management ===
  public List<FileServerPlugin> getEnabledPlugins() {
    return this.fileServerPlugins.stream()
        .filter(plugin -> pluginService.isPluginEnabled(plugin, ENABLED_FILESERVERS))
        .collect(Collectors.toList());
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
    for (FileServerPlugin plugin : getEnabledPlugins()) {
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
   * @throws PluginNotFoundException if pluginId not found
   */
  public void enablePlugin(@NotNull final UUID pluginId) {
    getPluginById(pluginId)
        .ifPresentOrElse(
            plugin -> pluginService.enablePlugin(plugin, ENABLED_FILESERVERS),
            () -> {
              throw new PluginNotFoundException(
                  "Trying to enable non-existent FileServerPlugin: " + pluginId);
            });
  }

  /**
   * Disable a specific file server plugin
   *
   * @param pluginId The ID of the plugin
   * @throws PluginNotFoundException if pluginId not found
   */
  public void disablePlugin(@NotNull final UUID pluginId) {
    getPluginById(pluginId)
        .ifPresentOrElse(
            plugin -> pluginService.disablePlugin(plugin, ENABLED_FILESERVERS),
            () -> {
              throw new PluginNotFoundException(
                  "Cannot disable non-existent FileServerPlugin: " + pluginId);
            });
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
      // Get a logged-in user
      final FileServerUser downloadUser = getDownloadUser(pluginForUrl.getPluginId());

      if (downloadUser != null) {
        final Set<HttpCookie> httpCookies = getHttpCookies(downloadUser);
        // Use the FS plugin to get the internal (download) URL
        return pluginForUrl.getDownloadURL(externalUrl, httpCookies);
      } else {
        throw new IOException("No logged in user could download requested URL: " + externalUrl);
      }
    } else {
      throw new IOException("Could not find plugin matching URL: " + externalUrl);
    }
  }

  public @NotNull Set<HttpCookie> getHttpCookies(@NotNull FileServerUser downloadUser) {
    return downloadUser.getCookies().stream()
        .map(secureDataService::decryptData)
        .map(SecureCookie::toSpringCookie)
        .collect(Collectors.toSet());
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
    for (final FileServerPlugin plugin : getEnabledPlugins()) {
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
    if (!users.isEmpty()) {
      // Return the download user
      return users.get(0);
    }
    // No logged-in users for this plugin
    return null;
  }
}
