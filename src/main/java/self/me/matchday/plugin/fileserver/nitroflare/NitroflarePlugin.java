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

package self.me.matchday.plugin.fileserver.nitroflare;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import self.me.matchday.model.MD5String;
import self.me.matchday.plugin.fileserver.FileServerPlugin;
import self.me.matchday.plugin.fileserver.FileServerUser;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class NitroflarePlugin implements FileServerPlugin {

  public static final String LOGIN_REQUEST_FORMAT = "email=%s&password=%s&login=&token=%s";

  private final NitroflarePluginProperties pluginProperties;
  private final WebClient webClient;
  private final Pattern urlPattern;

  @Autowired
  public NitroflarePlugin(final NitroflarePluginProperties pluginProperties) {

    this.pluginProperties = pluginProperties;
    this.webClient = WebClient.create(pluginProperties.getBaseUrl());
    this.urlPattern = Pattern.compile(pluginProperties.getUrlPattern());
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

  // === File Server Plugin ===
  @Override
  public @NotNull ClientResponse login(@NotNull FileServerUser user) {

    final String loginUrl = pluginProperties.getLoginUrl();
    // Encode login data
    final String loginRequestString = getLoginRequestString(user);
    // POST login request
    final ClientResponse clientResponse =
        webClient
            .post()
            .uri(loginUrl)
            .header("User-Agent", pluginProperties.getUserAgent())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(loginRequestString)
            .exchange()
            .block();
    // Ensure some kind of response is returned
    if (clientResponse == null) {
      return
          ClientResponse
              .create(HttpStatus.BAD_REQUEST)
              .body("Response from Nitroflare server was null")
              .build();
    }
    return clientResponse;
  }

  @Override
  public boolean acceptsUrl(@NotNull URL url) {
    return
        urlPattern
            .matcher(url.toString())
            .find();
  }

  @Override
  public @NotNull Duration getRefreshRate() {
    return
        Duration.ofHours(pluginProperties.getRefreshRateHours());
  }

  @Override
  public Optional<URL> getDownloadURL(@NotNull URL url, @NotNull Collection<HttpCookie> cookies)
      throws IOException {

    // Download link page
    final ClientResponse clientResponse =
        webClient
            .get()
            .uri(url.toString())
            .cookies(
                requestCookies -> cookies
                    .forEach(cookie -> requestCookies.add(cookie.getName(), cookie.getValue())))
            .exchange()
            .block();

    if (clientResponse != null && clientResponse.statusCode().is2xxSuccessful()) {
      // Extract HTML from response
      final String html = clientResponse.bodyToMono(String.class).block();
      // Parse link from page
      if (html != null) {
        return parseDownloadPage(html);
      }
    }
    // Link extraction unsuccessful
    return Optional.empty();
  }

  // === Helpers ===
  private Optional<URL> parseDownloadPage(@NotNull final String html) throws IOException {

    // Parse HTML into DOM
    final Document document = Jsoup.parse(html);
    // Find download link
    final Element link = document.getElementById(pluginProperties.getDownloadLinkId());
    if (link != null) {
      final String downloadUrl = link.attr("href");
      return
          Optional.of(new URL(downloadUrl));
    }
    // Link not found
    return Optional.empty();
  }

  private String getLoginRequestString(@NotNull final FileServerUser fileServerUser) {

    // Encode username
    final String username = URLEncoder.encode(fileServerUser.getUsername(), StandardCharsets.UTF_8);
    // Generate random token
    final String token = MD5String.generate();
    // Return formatted request String
    return
        String.format(LOGIN_REQUEST_FORMAT, username, fileServerUser.getPassword(), token);
  }
}
