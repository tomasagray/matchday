/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.fileserver;

import java.net.URL;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a file server management service. Can login/logout and translate download requests.
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
   * Return a Pattern which matches URLs this file server is capable of decoding.
   * @return A Pattern
   */
  Pattern getUrlMatcher();

  /**
   * Extract download URL data from a given URL
   * @param url The external access URL for the file server.
   * @return The internal URL needed to access this file resource from the outside world.
   */
  Optional<URL> getDownloadURL(@NotNull URL url);
}
