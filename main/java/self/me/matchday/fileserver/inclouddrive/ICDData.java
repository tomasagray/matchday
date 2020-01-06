/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.fileserver.inclouddrive;

import java.net.MalformedURLException;
import java.net.URL;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Log;

/** Helper class to hold data relevant to file-server (url, options, etc.) */
public final class ICDData {

  private static final String LOG_TAG = "ICDDataClass";

  // File server URL
  private static URL url;

  // User/access data
  private static final int me = 0;
  private static final String app = "br68ufmo5ej45ue1q10w68781069v666l2oh1j2ijt94";
  private static final String accessToken = "";

  // URL data
  private static final String protocol = "https://";
  private static final String domain = "inclouddrive.com";
  private static final String subDomain = "www";
  private static final String baseURL = protocol + subDomain + "." + domain + "/";
  private static final String loginUrl =
      baseURL + "api/" + me + "/signmein?useraccess=" + accessToken + "&access_token=" + app;
  private static final String FILE_URL_STUB = "https://www.inclouddrive.com/file/";

  // Initialize file server URL
  static
  {
    try {
      url = new URL(loginUrl);
    } catch (MalformedURLException e) {
      Log.e(LOG_TAG, "Error parsing InCloudDrive data!", e);
    }
  }

  @Contract(pure = true)
  public static String getDomain() {
    return domain;
  }

  @NotNull
  @Contract(pure = true)
  public static URL getLoginURL() {
    return url;
  }

  @Contract(pure = true)
  public static String getFileUrlStub() {
    return FILE_URL_STUB;
  }
}
