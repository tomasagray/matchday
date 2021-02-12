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

import org.brotli.dec.BrotliInputStream;
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
import java.util.zip.GZIPInputStream;

@Component
public class FileFoxPlugin implements FileServerPlugin {

  private static final String LOG_TAG = "FileFoxPlugin";

  private static final String HREF = "href";
  private final FileFoxPluginProperties pluginProperties;
  private final Pattern linkUrlPattern;
  private final Pattern directDownloadUrlPattern;
  private final Pattern downloadLimitPattern;
  private final CookieManager cookieManager;

  public FileFoxPlugin(@Autowired FileFoxPluginProperties pluginProperties) {

    this.pluginProperties = pluginProperties;
    this.linkUrlPattern = Pattern.compile(pluginProperties.getLinkUrlPattern());
    this.directDownloadUrlPattern = Pattern.compile(pluginProperties.getDirectDownloadUrlPattern());
    this.downloadLimitPattern = Pattern.compile(pluginProperties.getDownloadLimitPattern());

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

    // todo
    return ClientResponse.create(HttpStatus.BAD_REQUEST)
        .body("Login has not been implemented for FileFox yet. Sorry.")
        .build();
  }

  @Override
  public Optional<URL> getDownloadURL(@NotNull URL url, @NotNull Set<HttpCookie> cookies)
      throws IOException {

    // Read remote page
    String downloadLandingHtml = readDownloadPage(url, cookies);
    validateDownloadPage(downloadLandingHtml);

    // Get hidden input fields
    final Map<String, String> queryParams = getHiddenQueryParams(downloadLandingHtml);
    final URL formUrl = getHiddenFormUrl(url, downloadLandingHtml);

    // Fetch direct download page & parse
    final HttpURLConnection connection = (HttpURLConnection) setupUrlConnection(formUrl, cookies);
    final String directDownloadHtml = readDirectDownloadLink(connection, queryParams);
    return parseDirectDownloadPage(directDownloadHtml);
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

  private URL getHiddenFormUrl(@NotNull URL url, @NotNull String pageHtml) {

    final String errorMessage = "Could not parse direct download location from URL: " + url;
    try {
      // Get form URL
      final Document document = Jsoup.parse(pageHtml);
      final Optional<Element> formOptional = document.getElementsByTag("form").stream().findFirst();
      if (formOptional.isPresent()) {
        final Element hiddenForm = formOptional.get();
        final String submitUrl = hiddenForm.attr("action");
        return url.toURI().resolve(submitUrl).toURL();
      }
      // else...
      throw new IllegalArgumentException(errorMessage);
    } catch (URISyntaxException | MalformedURLException e) {
      // Wrap
      throw new IllegalArgumentException(errorMessage, e);
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
    final InputStream inputStream = getResponseInputStream(connection);
    final String response = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    Log.i(LOG_TAG, "Read response:\n" + response);
    // Close streams
    inputStream.close();
    os.close();
    return response;
  }

  private Optional<URL> parseDirectDownloadPage(@NotNull final String directDownloadHtml) {

    // Parse download link from response
    final Document document = Jsoup.parse(directDownloadHtml);
    // Check for download limit exceeded
    final Elements divs = document.select("div.info-box");
    divs.forEach(
        div -> {
          final String text = div.text();
          if (downloadLimitPattern.matcher(text).find()) {
            throw new RuntimeException(text);
          }
        });
    final Elements links = document.getElementsByTag("a");
    return links.stream()
        .filter(this::isDirectDownloadLink)
        .findFirst()
        .map(element -> element.attr(HREF))
        .map(
            href -> {
              try {
                return new URL(href);
              } catch (MalformedURLException e) {
                throw new RuntimeException(e);
              }
            });
  }

  private URLConnection setupUrlConnection(
      @NotNull URL url, @NotNull Collection<HttpCookie> cookies) throws IOException {

    final URLConnection connection = url.openConnection();
    final String cookieHeader = getCookieHeader(cookies);
    Log.i(
        LOG_TAG,
        String.format("Setting up HTTP connection to:\n%s\nUsing cookies:\n%s", url, cookieHeader));
    connection.setRequestProperty("Cookie", cookieHeader);
    connection.setRequestProperty("User-Agent", pluginProperties.getUserAgent());
    return connection;
  }

  private @NotNull String getCookieHeader(@NotNull final Collection<HttpCookie> cookies) {
    return cookies.stream()
        .map(cookie -> String.format("%s=%s", cookie.getName(), cookie.getValue()))
        .collect(Collectors.joining("; "));
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

  private InputStream getResponseInputStream(@NotNull final HttpURLConnection connection)
      throws IOException {

    InputStream is;
    // Check for error response
    if (connection.getResponseCode() >= 400) {
      is = connection.getErrorStream();
    } else {
      is = connection.getInputStream();
    }
    // Handle content encoding
    final String encoding = connection.getHeaderField("content-encoding");
    if (encoding.equalsIgnoreCase("br")) {
      is = new BrotliInputStream(is);
    } else if (encoding.equalsIgnoreCase("gzip")) {
      is = new GZIPInputStream(is);
    }
    return is;
  }

  private void configureDirectDownloadConnection(@NotNull final HttpURLConnection connection)
      throws ProtocolException {

    connection.setRequestMethod("POST");
    connection.setDoOutput(true);
    connection.setRequestProperty(
        "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
    connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
    connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
    connection.setRequestProperty("charset", "utf-8");
    connection.setRequestProperty("Connection", "keep-alive");
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    connection.setRequestProperty("Host", "filefox.cc");
    connection.setRequestProperty("Origin", "https://filefox.cc");
    connection.setRequestProperty("sec-fetch-dest", "document");
    connection.setRequestProperty("sec-fetch-mode", "navigate");
    connection.setRequestProperty("sec-fetch-site", "same-origin");
    connection.setRequestProperty("sec-fetch-user", "?1");
    connection.setRequestProperty("TE", "Trailers");
    connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
  }

  private boolean isDirectDownloadLink(@NotNull final Element link) {
    return link.hasAttr(HREF) && directDownloadUrlPattern.matcher(link.attr(HREF)).find();
  }
}
