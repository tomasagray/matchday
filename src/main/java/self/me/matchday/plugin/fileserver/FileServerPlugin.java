/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.plugin.fileserver;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpCookie;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.plugin.Plugin;

/**
 * Represents a file server management service. Can log-in/logout and translate download requests.
 */
public interface FileServerPlugin extends Plugin {

  /**
   * Login to the file server
   *
   * @return The response from the remote file server
   */
  @NotNull ClientResponse login(@NotNull final FileServerUser user);

  /**
   * Determine whether this file server can translate the given URL into a download URL.
   *
   * @param url The URL to be tested.
   * @return True/false.
   */
  boolean acceptsUrl(@NotNull final URL url);

  /**
   * Get the maximum age before data retrieved by this fileserver manager should be considered
   * stale.
   *
   * @return The maximum age before data should be refreshed.
   */
  @NotNull Duration getRefreshRate();

  /**
   * Extract download URL data from a given URL
   *
   * @param url The external access URL for the file server.
   * @return The internal URL needed to access this file resource from the outside world.
   */
  Optional<URL> getDownloadURL(@NotNull final URL url,
      @NotNull final Collection<HttpCookie> cookies) throws IOException;

}
