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

package self.me.matchday.plugin.fileserver.filefox;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.plugin.fileserver.FileServerPlugin;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.util.Log;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class FileFoxPlugin implements FileServerPlugin {

  private static final String LOG_TAG = "FileFoxPlugin";

  public static final String HREF = "href";
  private final FileFoxPluginProperties pluginProperties;
  private final Pattern linkUrlPattern;
  private final Pattern directDownloadUrlPattern;
  private final CookieManager cookieManager;

  public FileFoxPlugin(@Autowired FileFoxPluginProperties pluginProperties) {

    this.pluginProperties = pluginProperties;
    this.linkUrlPattern = Pattern.compile(pluginProperties.getLinkUrlPattern());
    this.directDownloadUrlPattern = Pattern.compile(pluginProperties.getDirectDownloadUrlPattern());

    // Instantiate cookie manager
    cookieManager = new CookieManager();
    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    CookieHandler.setDefault(cookieManager);
  }

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

  @Override
  public boolean acceptsUrl(@NotNull URL url) {
    final Matcher urlMatcher = linkUrlPattern.matcher(url.toString());
    return urlMatcher.find();
  }

  @Override
  public @NotNull Duration getRefreshRate() {
    return Duration.ofHours(pluginProperties.getRefreshHours());
  }

  @Override
  public @NotNull ClientResponse login(@NotNull FileServerUser user) {

    return ClientResponse.create(HttpStatus.BAD_REQUEST)
        .body("Login has not been implemented for FileFox yet. Sorry.")
        .build();
  }

  @Override
  public Optional<URL> getDownloadURL(@NotNull URL url, @NotNull Set<HttpCookie> cookies)
      throws IOException {

    final String downloadPage = getDirectDownloadLink(url, cookies);
    final URL directDownloadUrl = new URL(downloadPage);
    return Optional.of(directDownloadUrl);
  }

  private String getDirectDownloadLink(@NotNull final URL url, @NotNull Set<HttpCookie> cookies)
      throws IOException {

    // Read remote page
    String downloadLandingHtml = readDownloadPage(url, cookies);
    Log.i(LOG_TAG, "Read download page:\n\n" + downloadLandingHtml);
    validateDownloadPage(downloadLandingHtml);

    // Get hidden input fields
    final Map<String, String> queryParams = getHiddenQueryParams(downloadLandingHtml);
    final URL parentUrl = getParentUrl(url);

    // Fetch direct download page
    final HttpURLConnection connection = (HttpURLConnection) setupUrlConnection(parentUrl, cookies);
    connection.setRequestProperty("Referer", url.toString());
    final String directDownloadHtml = readDirectDownloadLink(connection, queryParams);
    Log.i(LOG_TAG, "Read direct download page:\n\n" + directDownloadHtml);

    // Parse download link from response
    final Document document = Jsoup.parse(directDownloadHtml);
    final Elements links = document.getElementsByTag("a");
    return links.stream()
        .filter(this::isDirectDownloadLink)
        .findFirst()
        .map(element -> element.attr(HREF))
        .orElseThrow(() -> new RuntimeException("Could not read download link from URL: " + url));
  }

  private boolean isDirectDownloadLink(Element link) {
    return link.hasAttr(HREF) && directDownloadUrlPattern.matcher(link.attr(HREF)).find();
  }

  private String readDownloadPage(@NotNull URL url, @NotNull Set<HttpCookie> cookies)
      throws IOException {

    final URLConnection connection = setupUrlConnection(url, cookies);
    // Read page with cookies
    try (final InputStreamReader isr = new InputStreamReader(connection.getInputStream());
        final BufferedReader reader = new BufferedReader(isr)) {
      updateCookies(cookies);
      return reader.lines().collect(Collectors.joining("\n"));
    }
  }

  private void updateCookies(@NotNull final Set<HttpCookie> cookies) {

    cookieManager.getCookieStore().getCookies().stream()
        .map(
            cookie ->
                ResponseCookie.from(cookie.getName(), cookie.getValue())
                    .domain(cookie.getDomain())
                    .maxAge(cookie.getMaxAge())
                    .path(cookie.getPath())
                    .secure(cookie.getSecure())
                    .httpOnly(cookie.isHttpOnly())
                    .build())
        .forEach(
            cookie -> {
              cookies.remove(cookie);
              cookies.add(cookie);
            });
  }

  private void validateDownloadPage(@NotNull final String html) {

    final String linkButtonText = "Get Download Link";
    final Document downloadPage = Jsoup.parse(html);
    final Elements buttons = downloadPage.select("button.btn-default");
    // Find the "get download link" button
    buttons.stream()
        .filter(button -> button.text().equalsIgnoreCase(linkButtonText))
        .findAny()
        .orElseThrow(
            () -> new RuntimeException("HTML is not a valid download page (not logged in?)"));
  }

  private URLConnection setupUrlConnection(
      @NotNull URL url, @NotNull Collection<HttpCookie> cookies) throws IOException {

    final URLConnection connection = url.openConnection();
    final String cookieHeader = getCookieHeader(cookies);
    Log.i(LOG_TAG, "Setting up HTTP connection using cookies:\n" + cookieHeader);
    connection.setRequestProperty("Cookie", cookieHeader);
    connection.setRequestProperty("User-Agent", pluginProperties.getUserAgent());
    return connection;
  }

  private InputStream getPostResponseInputStream(@NotNull HttpURLConnection connection)
      throws IOException {
    if (connection.getResponseCode() >= 400) {
      return connection.getErrorStream();
    } else {
      return connection.getInputStream();
    }
  }

  private String readDirectDownloadLink(
      @NotNull HttpURLConnection connection, Map<String, String> queryParams) throws IOException {

    configureDirectDownloadConnection(connection);
    // Get query string
    final String query =
        queryParams.entrySet().stream()
            .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining("&"));
    final byte[] queryBytes = query.getBytes(StandardCharsets.UTF_8);
    connection.setRequestProperty("Content-Length", Integer.toString(query.length()));
    Log.i(LOG_TAG, "Using query string: " + query);

    // POST request for download link
    final OutputStream os = connection.getOutputStream();
    os.write(queryBytes);
    os.flush();

    // Read complete response
    final InputStream inputStream = getPostResponseInputStream(connection);
    final String response = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    Log.i(LOG_TAG, "Read response:\n" + response);
    // Close streams
    inputStream.close();
    os.close();
    return response;
  }

  private void configureDirectDownloadConnection(@NotNull HttpURLConnection connection)
      throws ProtocolException {

    connection.setRequestMethod("POST");
    connection.setDoOutput(true);
    connection.setRequestProperty(
        "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
//    connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
    connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
    connection.setRequestProperty("charset", "utf-8");
    connection.setRequestProperty("Connection", "keep-alive");
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    connection.setRequestProperty("Host", "filefox.cc");
    connection.setRequestProperty("Origin", "https://filefox.cc");
    connection.setRequestProperty("TE", "Trailers");
    connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
  }

  private @NotNull String getCookieHeader(@NotNull Collection<HttpCookie> cookies) {
    return cookies.stream()
        .map(cookie -> String.format("%s=%s", cookie.getName(), cookie.getValue()))
        .collect(Collectors.joining("; "));
  }

  private Map<String, String> getHiddenQueryParams(String html) {

    // Parse download page for hidden form
    final Document downloadPage = Jsoup.parse(html);
    final FormElement hiddenForm = downloadPage.getAllElements().forms().get(0);
    final Elements hiddenInputs = hiddenForm.select("input[type=hidden]");
    // Parse inputs
    final Map<String, String> hiddenValues = new LinkedHashMap<>();
    hiddenInputs.forEach(
        element -> {
          final String name = element.attr("name");
          final String value = element.attr("value");
          hiddenValues.put(name, value);
        });
    return hiddenValues;
  }

  private URL getParentUrl(@NotNull URL url) {

    final String errorMessage = String.format("URL: %s is not a valid FileFox download link", url);
    try {
      if (linkUrlPattern.matcher(url.toString()).find()) {
        final URI uri = url.toURI();
        final URI downloadLink = uri.resolve(".");
        // Remove trailing slash
        String urlString = downloadLink.toString();
        if (urlString.endsWith("/")) {
          urlString = urlString.substring(0, urlString.length() - 2);
        }
        return new URL(urlString);
      }
      // else...
      throw new IllegalArgumentException(errorMessage);
    } catch (URISyntaxException | MalformedURLException e) {
      // Wrap
      throw new IllegalArgumentException(errorMessage, e);
    }
  }
}
