/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.plugin.fileserver.inclouddrive;

import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.CookieService;
import self.me.matchday.util.Log;

/**
 * Represents a CookieManager specific to the InCloudDrive cloud file storage service.
 */
@Component
class ICDCookieManager extends CookieManager {

  private static final String LOG_TAG = "ICDCookieManager";

  private static final URI DOMAIN = URI.create(".inclouddrive.com");
  private static final String USERDATA = "userdata";

  private final CookieService cookieService;

  @Autowired
  ICDCookieManager(@NotNull final CookieService cookieService) {

    // Save CookieService reference
    this.cookieService = cookieService;
    // Setup cookie policy
    setCookiePolicy(new ICDCookiePolicy());
    // Load previously saved cookies
    loadCookies();
  }


  /**
   * Load previously saved cookie data.
   */
  private void loadCookies() {
    // Load persisted cookies
    cookieService
        .fetchAll()
        .forEach(cookie -> getCookieStore().add(DOMAIN, cookie));
  }

  /**
   * Add a cookie to the Cookie Store
   *
   * @param data Cookie data for the user data cookie
   */
  void saveUserDataCookie(@NotNull String data) {

    try {
      // Encode data
      String encodedData = URLEncoder.encode(data, StandardCharsets.UTF_8.toString());
      // Create UserData cookie
      HttpCookie cookie = new HttpCookie(USERDATA, encodedData);
      cookie.setDomain(DOMAIN.toString());
      // Add to store
      getCookieStore().add(DOMAIN, cookie);
      // Persist cookie
      saveCookieData();

    } catch (IOException e) {
      Log.e(LOG_TAG, "Could not save cookie to the cookie store", e);
    }
  }

  /**
   * Save cookie data to persistent storage for later
   * retrieval.
   */
  void saveCookieData() {
    getCookieStore().getCookies().forEach(cookieService::saveCookie);
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
