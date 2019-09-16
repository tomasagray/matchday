package self.me.matchday.fileserver;

import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import self.me.matchday.util.Log;

class ICDCookiePolicy implements CookiePolicy {
  private static final String LOG_TAG = "ICDCookiePolicy";

  // Other InCloudDrive URLs
  private final String altURL;

  @Override
  public boolean shouldAccept(URI uri, HttpCookie cookie) {
    //        Log.d(LOG_TAG, "Checking uri: " + uri.toString());

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

  public ICDCookiePolicy(String url) {
    altURL = url;
  }
}
