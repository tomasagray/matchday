/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.fileserver.inclouddrive;

import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Log;

/**
 * Represents a CookieManager specific to the InCloudDrive cloud file storage service.
 */
class ICDCookieManager extends CookieManager {

  private static final String LOG_TAG = "ICDCookieManager";

  ICDCookieManager() {
    // Setup cookie policy
    setCookiePolicy(new ICDCookiePolicy(ICDData.getDomain()));
  }

  /** Add a cookie to the Cookie Store */
  void addCookie(@NotNull final String name, @NotNull String data) {

    try {
      // Encode data
      String encodedData = URLEncoder.encode(data, StandardCharsets.UTF_8.toString());
      // Create cookie
      HttpCookie cookie = new HttpCookie(name, encodedData);
      cookie.setDomain("." + ICDData.getDomain());
      // Add to store
      getCookieStore().add(new URI(cookie.getDomain()), cookie);

    } catch (URISyntaxException | UnsupportedEncodingException e) {
      Log.e(LOG_TAG, "Could not save cookie to the cookie store", e);
    }
  }

  /*
   * Save cookie data to persistent storage for later
   * retrieval.
   */
  boolean saveCookieData() {
    // TODO: Implement persistent cookie storage
    return false;
  }

  /*
   * Load previously saved cookie data.
   */
  Optional<List<HttpCookie>> loadCookieData() {
    // We may not have any data saved yet.
    // TODO: Implement persistent cookie loading
    return Optional.empty();
  }

  @NotNull
  String getCookieString() {
    // Container for cookie String
    StringBuilder sb = new StringBuilder();
    // Our cookies
    List<HttpCookie> cookies = this.getCookieStore().getCookies();
    // Add each cookie to String
    cookies.forEach(
        (cookie) ->
            sb.append(cookie.getName())
                .append("=")
                .append(cookie.getValue())
                .append("; ") // separator
        );

    // Return assembled String
    return sb.toString();
  }
}
