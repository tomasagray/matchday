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

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import net.tomasbot.matchday.model.FileServerUser;
import net.tomasbot.matchday.plugin.fileserver.FileServerPlugin;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;

@Component
public class FileFoxPlugin implements FileServerPlugin {

  private final FileFoxPluginProperties pluginProperties;
  private final LoginParser loginParser;
  private final DownloadParser downloadParser;
  private final BandwidthParser bandwidthParser;

  public FileFoxPlugin(
      FileFoxPluginProperties pluginProperties,
      LoginParser loginParser,
      DownloadParser downloadParser,
      BandwidthParser bandwidthParser) {
    this.pluginProperties = pluginProperties;
    this.loginParser = loginParser;
    this.downloadParser = downloadParser;
    this.bandwidthParser = bandwidthParser;
  }

  private static @NotNull LinkedMultiValueMap<String, String> getCookieJar(
      @NotNull Collection<HttpCookie> cookies) {
    final LinkedMultiValueMap<String, String> cookieJar = new LinkedMultiValueMap<>();
    cookies.forEach(cookie -> cookieJar.add(cookie.getName(), cookie.getValue()));
    return cookieJar;
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
  public @NotNull URL getHostname() {
    return pluginProperties.getBaseUrl();
  }

  @Override
  public @NotNull Duration getRefreshRate() {
    return Duration.ofHours(pluginProperties.getRefreshHours());
  }

  @Override
  public boolean acceptsUrl(@NotNull URL url) {
    final Matcher urlMatcher = pluginProperties.getLinkUrlPattern().matcher(url.toString());
    return urlMatcher.find();
  }

  @Override
  public @NotNull ClientResponse login(@NotNull FileServerUser user) {
    return loginParser.performLogin(user);
  }

  @Override
  public Optional<URL> getDownloadURL(@NotNull URL url, @NotNull Set<HttpCookie> cookies)
      throws IOException {
    // force HTTPS
    final String httpsUrl = url.toString().replaceFirst("^http:", "https:");
    final URI uri = URI.create(httpsUrl);

    // add cookies
    final LinkedMultiValueMap<String, String> cookieJar = getCookieJar(cookies);

    final URL downloadUrl = downloadParser.parseDownloadRequest(uri, cookieJar);
    return Optional.of(downloadUrl);
  }

  @Override
  public float getRemainingBandwidth(@NotNull Set<HttpCookie> cookies) throws IOException {
    LinkedMultiValueMap<String, String> cookieJar = getCookieJar(cookies);
    return bandwidthParser.getRemainingBandwidth(cookieJar);
  }
}
