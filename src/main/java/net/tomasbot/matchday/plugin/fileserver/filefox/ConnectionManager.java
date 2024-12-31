/*
 * Copyright (c) 2022.
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

package net.tomasbot.matchday.plugin.fileserver.filefox;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.brotli.dec.BrotliInputStream;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;

@Component
public class ConnectionManager {

  private static final String USER_AGENT = "User-Agent";

  private final FileFoxPluginProperties pluginProperties;

  public ConnectionManager(FileFoxPluginProperties pluginProperties) {
    this.pluginProperties = pluginProperties;
  }

  public ClientResponse connectTo(
      @NotNull URI uri, @NotNull MultiValueMap<String, String> cookieJar) throws IOException {
    return connectTo(uri, cookieJar, 0);
  }

  private @NotNull ClientResponse connectTo(
      @NotNull URI uri, @NotNull MultiValueMap<String, String> cookieJar, int currentDepth)
      throws IOException {
    final ClientResponse response = get(uri, cookieJar);
    final HttpStatus status = response.statusCode();
    final boolean shouldContinue = currentDepth < pluginProperties.getMaxRedirectDepth();
    if (status.is3xxRedirection() && shouldContinue) {
      final List<String> locations = response.headers().asHttpHeaders().get("Location");
      if (locations != null && !locations.isEmpty()) {
        final URI redirectUrl = URI.create(locations.get(0));
        return connectTo(redirectUrl, cookieJar, ++currentDepth);
      } else {
        throw new IOException("No redirect URL (Location header) provided in 3xx response");
      }
    }
    return response;
  }

  public ClientResponse get(
      @NotNull final URI uri, @NotNull final MultiValueMap<String, String> cookies)
      throws IOException {
    final URL url = uri.toURL();
    final HttpURLConnection connection = setupUrlConnection(url, cookies);
    return readHttpData(connection);
  }

  public ClientResponse post(
      @NotNull URI uri,
      @NotNull final MultiValueMap<String, String> cookies,
      @NotNull final Map<String, String> queryParams) {
    try {
      final URL url = uri.toURL();
      final String query = getQueryString(queryParams);
      final HttpURLConnection connection = setupUrlConnection(url, cookies);
      return performPost(connection, query);

    } catch (IOException e) {
      return ClientResponse.create(HttpStatus.BAD_REQUEST).body(e.getMessage()).build();
    }
  }

  private @NotNull HttpURLConnection setupUrlConnection(
      @NotNull URL url, @NotNull MultiValueMap<String, String> cookies) throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    final String cookiesHeader = getCookiesHeader(cookies);
    if (!cookiesHeader.isEmpty()) {
      connection.setRequestProperty("Cookie", cookiesHeader);
    }
    connection.setRequestProperty(USER_AGENT, pluginProperties.getUserAgent());
    return connection;
  }

  /**
   * Read data from an HTTP connection and return the response, with headers
   *
   * @param connection an opened HTTP connection
   * @return the response from the server
   * @throws IOException if we cannot connect/read
   */
  private @NotNull ClientResponse readHttpData(@NotNull final HttpURLConnection connection)
      throws IOException {
    try (final InputStreamReader isr = new InputStreamReader(getInputStream(connection));
        final BufferedReader reader = new BufferedReader(isr)) {

      final HttpStatus status = HttpStatus.valueOf(connection.getResponseCode());
      // Get headers, removing null entries
      final Map<String, List<String>> headers =
          connection.getHeaderFields().entrySet().stream()
              .filter(entry -> entry.getKey() != null)
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      final String body = reader.lines().collect(Collectors.joining("\n"));
      
      return ClientResponse.create(status)
          .headers(responseHeaders -> responseHeaders.putAll(headers))
          .body(body)
          .build();
    }
  }

  private @NotNull ClientResponse performPost(
      @NotNull HttpURLConnection connection, @NotNull String query) throws IOException {
    // attach headers, other config
    configurePostConnection(connection, query);

    // POST request for download link
    final OutputStream os = connection.getOutputStream();
    final byte[] queryBytes = query.getBytes(StandardCharsets.UTF_8);
    os.write(queryBytes);
    os.flush();

    // read complete response
    final ClientResponse response = readHttpData(connection);
    os.close();

    return response;
  }

  @NotNull
  private String getCookiesHeader(@NotNull MultiValueMap<String, String> cookies) {
    return cookies.toSingleValueMap().entrySet().stream()
        .map(cookie -> String.format("%s=%s", cookie.getKey(), cookie.getValue()))
        .collect(Collectors.joining("; "));
  }

  @NotNull
  private String getQueryString(@NotNull Map<String, String> queryParams) {
    return queryParams.entrySet().stream()
        .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
        .collect(Collectors.joining("&"));
  }

  private InputStream getInputStream(@NotNull final HttpURLConnection connection)
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
    if ("br".equalsIgnoreCase(encoding)) {
      is = new BrotliInputStream(is);
    } else if ("gzip".equalsIgnoreCase(encoding)) {
      is = new GZIPInputStream(is);
    }
    return is;
  }

  private void configurePostConnection(@NotNull HttpURLConnection connection, @NotNull String query)
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
    connection.setRequestProperty("Content-Length", Integer.toString(query.length()));
  }
}
