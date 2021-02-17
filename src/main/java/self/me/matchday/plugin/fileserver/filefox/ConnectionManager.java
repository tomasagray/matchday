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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.util.Log;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Component
public class ConnectionManager {

  private static final String USER_AGENT = "User-Agent";
  private static final SimpleDateFormat dateFormat =
      new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz");

  private final FileFoxPluginProperties pluginProperties;

  public ConnectionManager(@Autowired FileFoxPluginProperties pluginProperties) {
    this.pluginProperties = pluginProperties;
  }

  @Transactional
  public ClientResponse get(
      @NotNull final URI uri, @NotNull final MultiValueMap<String, String> cookies) {

    try {
      // Map data
      final URL url = uri.toURL();
      final URLConnection connection = setupUrlConnection(url, cookies);
      final String data = readHttpData(connection);

      return ClientResponse.create(HttpStatus.OK)
          .cookies(responseCookies -> updateCookies(connection, responseCookies))
          .body(data)
          .build();
    } catch (IOException e) {
      return ClientResponse.create(HttpStatus.BAD_REQUEST).body(e.getMessage()).build();
    }
  }

  @Transactional
  public ClientResponse post(
      @NotNull URI uri,
      @NotNull final MultiValueMap<String, String> cookies,
      @NotNull final Map<String, String> queryParams) {

    try {
      final URL url = uri.toURL();
      final String query = getQueryString(queryParams);
      final HttpURLConnection connection = setupUrlConnection(url, cookies);
      final String response = readPostResponse(connection, query);

      return ClientResponse.create(HttpStatus.OK).body(response).build();

    } catch (IOException e) {
      return ClientResponse.create(HttpStatus.BAD_REQUEST).body(e.getMessage()).build();
    }
  }

  private HttpURLConnection setupUrlConnection(
      @NotNull URL url, @NotNull MultiValueMap<String, String> cookies) throws IOException {

    final String cookiesHeader = getCookiesHeader(cookies);
    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestProperty("Cookie", cookiesHeader);
    connection.setRequestProperty(USER_AGENT, pluginProperties.getUserAgent());
    return connection;
  }

  private String readHttpData(@NotNull final URLConnection connection) throws IOException {

    // Read page with cookies
    try (final InputStreamReader isr = new InputStreamReader(connection.getInputStream());
        final BufferedReader reader = new BufferedReader(isr)) {
      return reader.lines().collect(Collectors.joining("\n"));
    }
  }

  @NotNull
  private String getCookiesHeader(MultiValueMap<String, String> cookies) {

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

  private void updateCookies(
      @NotNull final URLConnection connection,
      @NotNull final MultiValueMap<String, ResponseCookie> responseCookies) {

    connection.getHeaderFields().get("Set-Cookie").stream()
        .map(this::responseCookieFromString)
        .filter(Objects::nonNull)
        .forEach(cookie -> responseCookies.add(cookie.getName(), cookie));
  }

  private ResponseCookie responseCookieFromString(@NotNull final String str) {

    try {

      final List<List<String>> cookieFields =
          Arrays.stream(str.split("; "))
              .map(field -> Arrays.asList(field.split("=")))
              .collect(Collectors.toList());
      // Pop name & value
      final String name = cookieFields.get(0).get(0);
      final String value = cookieFields.get(0).get(1);
      cookieFields.remove(0);
      final Map<String, String> cookieMap =
          cookieFields.stream()
              .collect(Collectors.toMap(this::getCookieName, this::getCookieValue));

      final String domain = cookieMap.get("domain");
      final String path = cookieMap.get("path");
      final Date date = dateFormat.parse(cookieMap.get("expires"));
      final Duration maxAge = Duration.between(Instant.now(), date.toInstant());
      final boolean httpOnly = cookieMap.get("HttpOnly") != null;
      final boolean secure = cookieMap.get("Secure") != null;
      final String sameSite = cookieMap.get("SameSite");

      return ResponseCookie.from(name, value)
          .domain(domain)
          .path(path)
          .maxAge(maxAge)
          .httpOnly(httpOnly)
          .secure(secure)
          .sameSite(sameSite)
          .build();
    } catch (ParseException e) {
      Log.e("FFConnectionManager", "Could not parse response cookie", e);
      return null;
    }
  }

  private String readPostResponse(@NotNull HttpURLConnection connection, @NotNull String query)
      throws IOException {

    configurePostConnection(connection);
    final byte[] queryBytes = query.getBytes(StandardCharsets.UTF_8);
    connection.setRequestProperty("Content-Length", Integer.toString(query.length()));

    // POST request for download link
    final OutputStream os = connection.getOutputStream();
    os.write(queryBytes);
    os.flush();

    // Read complete response
    final InputStream inputStream = getResponseInputStream(connection);
    final String response = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    // Close streams
    inputStream.close();
    os.close();
    return response;
  }

  private void configurePostConnection(@NotNull final HttpURLConnection connection)
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
    if ("br".equalsIgnoreCase(encoding)) {
      is = new BrotliInputStream(is);
    } else if ("gzip".equalsIgnoreCase(encoding)) {
      is = new GZIPInputStream(is);
    }
    return is;
  }

  private String getCookieName(@NotNull final List<String> strs) {
    if (strs.size() > 0) {
      return strs.get(0);
    }
    return null;
  }

  private String getCookieValue(@NotNull final List<String> strs) {
    if (strs.size() > 1) {
      return strs.get(1);
    }
    return "true";
  }
}
