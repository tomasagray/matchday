/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.fileserver.inclouddrive;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Log;

/** Helper class to hold data relevant to file-server (url, options, etc.) */
public final class ICDData {

  private static final String LOG_TAG = "ICDDataClass";

  // File server login URL
  private static URL loginUrl;

  // User/access data
  private static final int me = 0;
  private static final String app = "br68ufmo5ej45ue1q10w68781069v666l2oh1j2ijt94";
  private static final String accessToken = "";

  static final String DOWNLOAD_LINK_IDENTIFIER = "downloadnow";
  static final String USER_DATA_IDENTIFIER = "doz";
  static final String USER_AGENT
      // Windows
      // = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)
      // Chrome/71.0.3578.98 Safari/537.36";
      // Mac
      =
      "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";


  // URL data
  private static final String FILE_URL_PATTERN = "https://www.inclouddrive.com/file/.*";
  private static final Pattern urlMatcher = Pattern.compile(FILE_URL_PATTERN);
  private static final String protocol = "https://";
  private static final String domain = "inclouddrive.com";
  private static final String subDomain = "www";
  private static final String baseURL = protocol + subDomain + "." + domain + "/";
  private static final String loginString =
      baseURL + "api/" + me + "/signmein?useraccess=" + accessToken + "&access_token=" + app;

  // Initialize file server URL
  static
  {
    try {
      loginUrl = new URL(loginString);
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
    return loginUrl;
  }

  public static Pattern getUrlMatcher() {
    return urlMatcher;
  }
}
