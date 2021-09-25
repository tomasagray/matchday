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

package self.me.matchday.plugin.fileserver;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpCookie;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.plugin.Plugin;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a file server management service. Can log-in/logout and translate download requests.
 */
public interface FileServerPlugin extends Plugin {

  /**
   * Get the hostname of the fileserver as a URL
   *
   * @return Hostname of fileserver
   */
  @NotNull
  URL getHostname();

  /**
   * Get the maximum age before data retrieved by this fileserver manager should be considered
   * stale.
   *
   * @return The maximum age before data should be refreshed.
   */
  @NotNull
  Duration getRefreshRate();

  /**
   * Login to the file server
   *
   * @return The response from the remote file server
   */
  @NotNull
  ClientResponse login(@NotNull final FileServerUser user);

  /**
   * Determine whether this file server can translate the given URL into a download URL.
   *
   * @param url The URL to be tested.
   * @return True/false.
   */
  boolean acceptsUrl(@NotNull final URL url);

  /**
   * Extract download URL data from a given URL
   *
   * @param url The external access URL for the file server.
   * @return The internal URL needed to access this file resource from the outside world.
   */
  Optional<URL> getDownloadURL(@NotNull final URL url, @NotNull final Set<HttpCookie> cookies)
      throws IOException;
}
