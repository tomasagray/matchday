/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.plugin.fileserver.inclouddrive;

import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Log;

/**
 * The cookie policy to be used against the InCloudDrive file service.
 */
class ICDCookiePolicy implements CookiePolicy {

  private static final String LOG_TAG = "ICDCookiePolicy";

  // Other InCloudDrive URLs
  private final String altURL;

  @Override
  public boolean shouldAccept(@NotNull final URI uri, @NotNull final HttpCookie cookie) {

    try {
      // Host name of URL to test
      String host = InetAddress.getByName(uri.getHost()).getCanonicalHostName();

      // Check if host name matches
      if (!(HttpCookie.domainMatches(altURL, host))) {
        return false;
      }

      // Apply default cookie policy rules & return
      return CookiePolicy.ACCEPT_ORIGINAL_SERVER.shouldAccept(uri, cookie);
    } catch (UnknownHostException e) {
      Log.e(LOG_TAG, "Unknown host exception: " + uri.toString());
      return false;
    }
  }

  public ICDCookiePolicy() {
    this.altURL = "inclouddrive.com";
  }
}
