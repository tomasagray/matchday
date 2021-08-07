/*
 * Copyright (c) 2021.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.plugin.fileserver.inclouddrive;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import self.me.matchday.plugin.fileserver.FileServerPlugin;
import self.me.matchday.plugin.fileserver.FileServerUser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class IcdPlugin implements FileServerPlugin {

  // Constants
  public static final String USER_AGENT_HEADER = "User-Agent";
  public static final String USERDATA_COOKIE_NAME = "userdata";
  public static final int COOKIE_MAX_DAYS = 10;
  public static final String COOKIE_DOMAIN = ".inclouddrive.com";

  // Dependencies
  private final IcdPluginProperties pluginProperties;
  private final WebClient webClient;
  private final Gson gson;
  // Fields
  private final Pattern acceptsUrlPattern;
  private final Duration refreshRate;

  @Autowired
  public IcdPlugin(@NotNull final IcdPluginProperties pluginProperties) {

    // initialize dependencies
    this.pluginProperties = pluginProperties;
    this.webClient = WebClient.create(pluginProperties.getBaseUrl().toString());
    gson = new Gson();

    // initialize fields
    acceptsUrlPattern = Pattern.compile(this.pluginProperties.getUrlPattern());
    refreshRate = Duration.ofHours(this.pluginProperties.getDefaultRefreshHours());
  }

  // === File server ===
  @Override
  public @NotNull URL getHostname() {
    return pluginProperties.getBaseUrl();
  }

  @Override
  public @NotNull Duration getRefreshRate() {
    return this.refreshRate;
  }

  @Override
  public @NotNull ClientResponse login(@NotNull FileServerUser user) {

    // TODO - refactor this method!
    // Result container
    ClientResponse result;

    // Get login URL as a String
    final String loginUrl = pluginProperties.getLoginUri();
    // Translate to raw byte array
    byte[] loginData = getLoginDataByteArray(user);

    try {
      // Attempt to login & return response
      final ClientResponse response =
          webClient
              .post()
              .uri(loginUrl)
              .header(USER_AGENT_HEADER, pluginProperties.getUserAgent())
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .bodyValue(loginData)
              .exchange()
              .block();

      // Null response
      if (response == null) {
        result =
            ClientResponse.create(HttpStatus.BAD_REQUEST)
                .body("Login failed! No response from server (response was null)")
                .build();
      }
      // Login attempt "successful"...
      else if (response.statusCode().is2xxSuccessful()) {

        // Parse response body
        final String responseBody = response.bodyToMono(String.class).block();
        final IcdMessage icdMessage = gson.fromJson(responseBody, IcdMessage.class);

        // Check for hidden error
        if ("error".equals(icdMessage.getResult())) {
          // It's actually an error, correct response & return
          return ClientResponse.from(response)
              .statusCode(HttpStatus.BAD_REQUEST)
              .body(icdMessage.getMessage())
              .build();
        }

        // Extract login cookie
        final String userData = URLEncoder.encode(icdMessage.getDoz(), StandardCharsets.UTF_8);
        final ResponseCookie userDataCookie =
            ResponseCookie.from(USERDATA_COOKIE_NAME, userData)
                .maxAge(Duration.ofDays(COOKIE_MAX_DAYS))
                .path("/")
                .domain(COOKIE_DOMAIN)
                .secure(true)
                .build();
        // Add user data cookie to response
        result =
            ClientResponse.from(response)
                .cookies(cookies -> cookies.add(USERDATA_COOKIE_NAME, userDataCookie))
                .build();
      } else {
        result = response;
      }
    } catch (JsonSyntaxException | NullPointerException e) {
      // Return useful response to end user
      final String message =
          String.format("Could not parse response from InCloudDrive: %s", e.getMessage());
      // Return corrected response
      result = ClientResponse.create(HttpStatus.BAD_REQUEST).body(message).build();
    }
    // Return finalized result
    return result;
  }

  @Override
  public boolean acceptsUrl(@NotNull URL url) {
    return acceptsUrlPattern.matcher(url.toString()).find();
  }

  @Override
  public Optional<URL> getDownloadURL(@NotNull URL url, @NotNull final Set<HttpCookie> cookies)
      throws IOException {

    // Create connection to ICD server
    final HttpURLConnection connection = createHttpConnection(url, cookies);
    // Read download page from connection
    final String downloadPage = readDownloadPage(connection);
    // Parse result & return
    return parseDownloadPage(downloadPage);
  }

  // === Plugin ===
  @Override
  public UUID getPluginId() {
    return UUID.fromString(pluginProperties.getId());
  }

  @Override
  public String getTitle() {
    return pluginProperties.getTitle();
  }

  @Override
  public String getDescription() {
    return pluginProperties.getDescription();
  }

  // === Helpers ===

  /**
   * Create & initialize an HTTP GET connection to the InCloudDrive file server.
   *
   * @param url The URL to which we want to connect
   * @param cookies Any cookies necessary for authentication
   * @return An initialized HttpURLConnection
   * @throws IOException If connecting goes awry
   */
  private @NotNull HttpURLConnection createHttpConnection(
      @NotNull URL url, @NotNull Collection<HttpCookie> cookies) throws IOException {

    // Create HttpConnection
    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");

    // set headers
    connection.setRequestProperty(USER_AGENT_HEADER, pluginProperties.getUserAgent());
    // combine cookies & set as request header
    final String cookieHeader =
        cookies.stream()
            .map(cookie -> String.format("%s=%s", cookie.getName(), cookie.getValue()))
            .collect(Collectors.joining("; "));
    connection.setRequestProperty("Cookie", cookieHeader);
    // Return initialized connection
    return connection;
  }

  /**
   * Read text data from a URLConnection
   *
   * @param connection The URL connection
   * @return A String containing data read from above connection
   * @throws IOException If there are problems reading data
   */
  private @NotNull String readDownloadPage(@NotNull final HttpURLConnection connection)
      throws IOException {

    // Result container
    final StringBuilder result = new StringBuilder();

    // connect
    connection.connect();
    // read data
    final BufferedReader reader =
        new BufferedReader(new InputStreamReader(connection.getInputStream()));
    String line;
    while ((line = reader.readLine()) != null) {
      result.append(line).append("\n");
    }
    // collate & return
    return result.toString();
  }

  /**
   * Parse a given String into HTML DOM and extract link to direct download.
   *
   * @param html The page HTML
   * @return An Optional of the download URL
   * @throws MalformedURLException If the parsed link is invalid
   */
  private @NotNull Optional<URL> parseDownloadPage(@NotNull final String html)
      throws MalformedURLException {

    // DOM-ify page
    Document filePage = Jsoup.parse(html);
    // Get all <a> with the link identifier class
    Elements elements = filePage.getElementsByClass(pluginProperties.getLinkIdentifier());
    // If we got a hit
    if (!elements.isEmpty()) {
      // Extract href from <a>
      final Element first = elements.first();
      assert first != null;
      String theLink = first.attr("href");
      return Optional.of(new URL(theLink));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Get an array of bytes for transmission
   *
   * @param user The user that will be logged into the file server
   * @return An array of bytes of the URL encoded String
   */
  private byte @NotNull [] getLoginDataByteArray(@NotNull FileServerUser user) {

    // Container for data
    StringJoiner sj = new StringJoiner("&");

    // Encode each data item and add it to the login
    // data String container
    sj.add(getURLComponent("password", user.getPassword()));
    sj.add("keep=1");
    sj.add(getURLComponent("email", user.getUsername()));

    // Assemble and return String data as a byte array
    return sj.toString().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Helper method for getLoginDataByteArray. URL encodes a given object.
   *
   * @param key The key used to retrieve the Object
   * @param value The object to be encoded
   * @return URL encoded String.
   */
  private @NotNull String getURLComponent(@NotNull final String key, final Object value) {

    final String k = URLEncoder.encode(key, StandardCharsets.UTF_8);
    final String v =
        (value != null) ? URLEncoder.encode(value.toString(), StandardCharsets.UTF_8) : "";
    return k + "=" + v;
  }
}
