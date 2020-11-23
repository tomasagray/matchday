/*
 * Copyright (c) 2020.
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
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import self.me.matchday.plugin.fileserver.FileServerPlugin;
import self.me.matchday.plugin.fileserver.FileServerUser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class IcdPlugin implements FileServerPlugin {

  public static final String USER_AGENT_HEADER = "User-Agent";
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
    this.webClient = WebClient.create(pluginProperties.getBaseUrl());
    gson = new Gson();

    // initialize fields
    acceptsUrlPattern = Pattern.compile(this.pluginProperties.getUrlPattern());
    refreshRate = Duration.ofHours(this.pluginProperties.getDefaultRefreshRate());
  }

  // === File server ===

  @Override
  public @NotNull ClientResponse login(@NotNull FileServerUser user) {

    // Get login URL as a String
    final String loginUrl = pluginProperties.getLoginUri();
    // Translate to raw byte array
    byte[] loginData = getLoginDataByteArray(user);

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

    // Correct response & return
    return correctIcdResponse(response);
  }

  @Override
  public boolean acceptsUrl(@NotNull URL url) {
    return acceptsUrlPattern.matcher(url.toString()).find();
  }

  @Override
  public @NotNull Duration getRefreshRate() {
    return this.refreshRate;
  }

  @Override
  public Optional<URL> getDownloadURL(
      @NotNull URL url, @NotNull final Collection<HttpCookie> cookies) throws IOException {

    // Result container
    Optional<URL> result = Optional.empty();

    // Get page via GET request
    final ClientResponse response =
        webClient
            .get()
            .uri(url.toString())
            .header(USER_AGENT_HEADER, pluginProperties.getUserAgent())
            .cookies(
                requestCookies -> {
                  // Map cookies
                  cookies.forEach(
                      cookie -> requestCookies.add(cookie.getName(), cookie.getValue()));
                })
            .exchange()
            .block();

    if (response != null) {
      // Extract body
      final String body = response.bodyToMono(String.class).block();

      if (body != null) {
        // Parse the returned HTML and get download link
        result = parseDownloadPage(body);
      }
    }

    // Return parsing result
    return result;
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
   * Extract message body & correct response code from ICD response.
   *
   * @param response The raw response from InCloudDrive
   * @return A corrected ClientResponse
   */
  private @NotNull ClientResponse correctIcdResponse(final ClientResponse response) {

    // Result containers
    HttpStatus trueStatus;
    String message;

    try {
      // Parse response body
      final String responseBody = response.bodyToMono(String.class).block();
      final IcdMessage icdMessage = gson.fromJson(responseBody, IcdMessage.class);

      // Extract result & message
      final String result = icdMessage.getResult();
      message = icdMessage.getMessage();
      trueStatus = response.statusCode(); // default

      // Correct response code - is it really an error?
      if (response.statusCode().is2xxSuccessful() && result.equals("error")) {
        // Yes!
        trueStatus = HttpStatus.FORBIDDEN;
      }
    } catch (JsonSyntaxException | NullPointerException e) {
      trueStatus = HttpStatus.BAD_GATEWAY;
      message = "Invalid response from server";
    }

    // Return corrected response
    return ClientResponse.create(trueStatus)
        .headers(httpHeaders -> httpHeaders.addAll(response.headers().asHttpHeaders()))
        .cookies(cookies -> cookies.addAll(response.cookies()))
        .body(message)
        .build();
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
      String theLink = elements.first().attr("href");
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
  private @NotNull byte[] getLoginDataByteArray(@NotNull FileServerUser user) {

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
