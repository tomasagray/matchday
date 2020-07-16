/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.plugin.fileserver;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.Plugin;

/**
 * Represents a file server management service. Can log-in/logout and translate download requests.
 */
public interface FileServerPlugin extends Plugin {

  // TODO: Make this return a ResponseEntity<> of cookies/response
  /**
   *  Login to the file server
   */
  boolean login(@NotNull FSUser user);

  /**
   * Logout of the file server
   */
  void logout();

  /**
   * Test whether logged in
   * @return True / false
   */
  boolean isLoggedIn();

  /**
   * Determine whether this file server can translate the given URL into a download URL.
   * @param url The URL to be tested.
   * @return True/false.
   */
  boolean acceptsUrl(@NotNull final URL url);

  /**
   * Get the maximum age before data retrieved by this fileserver manager should be considered stale.
   *
   * @return The maximum age before data should be refreshed.
   */
  Duration getRefreshRate();

  /**
   * Extract download URL data from a given URL
   * @param url The external access URL for the file server.
   * @return The internal URL needed to access this file resource from the outside world.
   */
  Optional<URL> getDownloadURL(@NotNull URL url) throws IOException;
}
