/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.fileserver.inclouddrive;

import static java.net.HttpURLConnection.HTTP_OK;
import static self.me.matchday.fileserver.inclouddrive.ICDData.DOWNLOAD_LINK_IDENTIFIER;
import static self.me.matchday.fileserver.inclouddrive.ICDData.USER_AGENT;
import static self.me.matchday.fileserver.inclouddrive.ICDData.USER_DATA_IDENTIFIER;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.StringJoiner;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.me.matchday.fileserver.FSUser;
import self.me.matchday.fileserver.IFSManager;
import self.me.matchday.io.JsonStreamReader;
import self.me.matchday.util.Log;

/**
 * Implementation of file server management for the InCloudDrive file service.
 */
@Component
public class ICDManager implements IFSManager {

  private static final String LOG_TAG = "ICDManager";

  // Fields
  private final ICDCookieManager cookieManager;
  private ICDUser user;
  private boolean isLoggedIn; // Current login status

  // Constructor
  @Autowired
  public ICDManager(@NotNull final ICDCookieManager cookieManager) {
    // Setup cookie management
    this.cookieManager = cookieManager;
    // Set default status to 'logged out'
    this.isLoggedIn = false;
  }


  // Login
  // ===============================================================================================
  /**
   * Perform user login and authentication to the file server, and save the returned cookies.
   * @param fsUser The file system user to be logged in
   * @return Login success (true/false)
   */
  @Override
  public boolean login(@NotNull FSUser fsUser) {

    // Create POST connection & attach request data
    try {
      // Get login data
      byte[] loginData = getLoginDataByteArray(fsUser);
      // Setup connection
      HttpURLConnection connection =
          setupICDPostConnection(ICDData.getLoginURL(), loginData.length);

      // Connect
      connection.connect();
      // POST login data
      postData(connection, loginData);

      if (connection.getResponseCode() == HTTP_OK) {
        // Read response as a JSON object
        final JsonObject loginResponse =
            JsonStreamReader.readJsonString(readServerResponse(connection));

        if (isLoginSuccessful(loginResponse)) {
          this.isLoggedIn = true;
          // Save user instance
          this.user = (ICDUser)fsUser;
          // Extract cookie from response, create userdata cookie
          cookieManager.saveUserDataCookie(loginResponse.get(USER_DATA_IDENTIFIER).getAsString());
          Log.i(LOG_TAG, "Successfully logged in user: " + user);

          // Login success!
          return true;
        } else {
          Log.e(
              LOG_TAG,
              String.format(
                  "Failed to login with user: %s; login response: %s",
                  user,
                  (loginResponse == null || loginResponse.isJsonNull())
                      ? null
                      : loginResponse.toString()));
        }
      } else {
        Log.e(
            LOG_TAG,
            String.format(
                "Could not connect to file server login service; response: [%s], %s",
                connection.getResponseCode(), connection.getResponseMessage()));
      }
    } catch (IOException e) {
      Log.e(LOG_TAG, "I/O Error while performing login function", e);
    }

    // Login FAILED!
    return false;
  }

  /**
   * Clear saved cookies, requiring the user to re-authenticate.
   */
  @Override
  public void logout() {
    // Delete ALL cookies
    cookieManager.getCookieStore().removeAll();
    // Clear the user
    this.user = null;
  }

  @Override
  public boolean isLoggedIn() {
    return this.isLoggedIn;
  }


  // Download
  // ===============================================================================================
  @Override
  public boolean acceptsUrl(@NotNull URL url) {
    return ICDData.getUrlMatcher(url.toString()).find();
  }

  /**
   * Extract the direct download link from the ICD page.
   *
   * @param url The URL of the ICD page
   * @return An Optional containing the DD URL, if found.
   */
  @Override
  public Optional<URL> getDownloadURL(@NotNull URL url) {
    // By default, empty container
    Optional<URL> downloadLink = Optional.empty();

    try {
      // Open a connection
      URLConnection connection = url.openConnection();

      // todo: is manual cookie attachment needed?
      // Attach cookies
      connection.setRequestProperty("Cookie", cookieManager.getCookieString());
      // Connect to file server
      connection.connect();

      // Read the page from the file server & DOM-ify it
      Document filePage = Jsoup.parse(readServerResponse(connection));
      // Get all <a> with the link identifier class
      Elements elements = filePage.getElementsByClass(DOWNLOAD_LINK_IDENTIFIER);
      // - If we got a hit
      if (!elements.isEmpty()) {
        // - Extract href from <a>
        String theLink = elements.first().attr("href");
        downloadLink = Optional.of(new URL(theLink));
      }
    } catch (IOException e) {
      Log.e(LOG_TAG, "Could not parse download link from supplied URL: " + url, e);
    }

    return downloadLink;
  }


  // Server
  // ===============================================================================================
  /**
   * Prepares an HttpURLConnection for a given URL, with a given POST data size.
   *
   * @param url      The URL we want a connection to
   * @param dataSize The size of the datagram that will be POSTed to this URL
   * @return HttpURLConnection A configured HTTP connection
   * @throws IOException If the connection cannot be opened.
   */
  @NotNull
  private HttpURLConnection setupICDPostConnection(@NotNull URL url, int dataSize)
      throws IOException {
    // Get an HTTP connection
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    // Set connection properties
    // - Make it a POST
    connection.setRequestMethod("POST");
    // - Enable I/O
    connection.setDoOutput(true);
    connection.setDoInput(true);
    // Set fixed data size
    connection.setFixedLengthStreamingMode(dataSize);
    connection.setRequestProperty(
        "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    // Set user agent (how we appear to server)
    connection.setRequestProperty("User-Agent", USER_AGENT);

    // Return the connection
    return connection;
  }

  /**
   * Send data via a POST connection to the server.
   *
   * @param connection The connection to the server. <i>Must already be opened.</i>
   * @param loginData  An array of bytes to be written to the server.
   */
  private void postData(@NotNull HttpURLConnection connection, byte[] loginData) {
    //   - POST login data to OutputStream
    try (OutputStream os = connection.getOutputStream()) {
      os.write(loginData);
    } catch (IOException e) {
      Log.e(LOG_TAG, "Could not write login data to output stream!", e);
    }
  }

  /**
   * Read the response from the server on a given connection
   *
   * @param connection The connection to read from
   * @return A String containing the response from the server
   * @throws IOException If there is an error reading the data
   */
  @NotNull
  private String readServerResponse(URLConnection connection) throws IOException {
    StringBuilder response = new StringBuilder();
    try (InputStream is = connection.getInputStream()) {
      int i;
      // Read the response, one byte at a time
      // until the end of the stream
      while ((i = is.read()) != -1) {
        response.append((char) i);
      }
    }

    // Assemble and return response
    return response.toString();
  }


  // Login
  // ===============================================================================================
  /**
   * Determine if login has been successfully performed.
   *
   * @return A boolean indicating if the last login attempt was successful.
   */
  private boolean isLoginSuccessful(JsonObject loginResponse) {

    // Assume the login will fail
    boolean loggedIn = false;

    if (loginResponse != null && !(loginResponse.isJsonNull())) {
      final JsonElement result = loginResponse.get("result");

      if (result != null && !(result.isJsonNull())) {
        final String loginResult = result.getAsString().trim().toUpperCase();
        if ("OK".equals(loginResult)) {
          loggedIn = true;
        }
      }
    }

    return loggedIn;
  }

  /**
   * Get an array of bytes for transmission
   *
   * @param user The user that will be logged into the file server
   * @return An array of bytes of the URL encoded String
   */
  @NotNull
  private static byte[] getLoginDataByteArray(@NotNull FSUser user) {
    // Container for data
    StringJoiner sj = new StringJoiner("&");

    // Encode each data item and add it to the login
    // data String container
    sj.add(getURLComponent("email", user.getUserName()));
    sj.add(getURLComponent("password", user.getPassword()));
    sj.add(getURLComponent("keep", user.isKeepLoggedIn()));

    // Assemble and return String data as a byte array
    return sj.toString().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Helper method for getLoginDataByteArray. URL encodes a given object.
   *
   * @param key   The key used to retrieve the Object
   * @param value The object to be encoded
   * @return URL encoded String.
   */
  @NotNull
  private static String getURLComponent(@NotNull String key, @NotNull Object value) {
    StringBuilder sb = new StringBuilder();
    String charset = StandardCharsets.UTF_8.toString();

    try {
      sb.append(URLEncoder.encode(key, charset))
          .append("=")
          .append(URLEncoder.encode(value.toString(), charset));

    } catch (UnsupportedEncodingException e) {
      Log.e(LOG_TAG, "Could not encode ICD user data", e);
    }

    return sb.toString();
  }
}
