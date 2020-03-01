/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.fileserver.inclouddrive;

import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.matchday._DEVFIXTURES.FAKECookieRepo;
import self.me.matchday.util.Log;

/**
 * Represents a CookieManager specific to the InCloudDrive cloud file storage service.
 */
@Component
class ICDCookieManager extends CookieManager {

  private static final String LOG_TAG = "ICDCookieManager";

  private static final URI DOMAIN = URI.create("." + ICDData.getDomain());
  private static final String USERDATA = "userdata";

  // cookie persistence
  // todo: delete, switch to real cookie repo
  private FAKECookieRepo cookieRepository = new FAKECookieRepo();

  ICDCookieManager() {
    // Setup cookie policy
    setCookiePolicy(new ICDCookiePolicy());

    // Load persisted cookies
    cookieRepository.findAll().forEach(cookie -> getCookieStore().add(DOMAIN, cookie));
  }

  /**
   * Add a cookie to the Cookie Store
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
      // todo: change to repo method
      cookieRepository.save(cookie);

    } catch (IOException e) {
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
