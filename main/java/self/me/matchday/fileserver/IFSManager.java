/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.fileserver;

import java.net.URL;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a file server management service. Can log-in/logout and translate download requests.
 */
public interface IFSManager {

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
   * Extract download URL data from a given URL
   * @param url The external access URL for the file server.
   * @return The internal URL needed to access this file resource from the outside world.
   */
  Optional<URL> getDownloadURL(@NotNull URL url);
}
