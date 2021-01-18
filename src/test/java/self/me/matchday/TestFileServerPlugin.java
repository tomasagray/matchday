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

package self.me.matchday;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.plugin.fileserver.FileServerPlugin;
import self.me.matchday.plugin.fileserver.FileServerUser;

import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Test implementation of file server plugin */
@Component
public class TestFileServerPlugin implements FileServerPlugin {

  private final UUID pluginId = UUID.fromString("4636e383-2ddb-477e-85f2-36e6cd7a434b");

  @Override
  public @NotNull ClientResponse login(@NotNull FileServerUser user) {

    final Pattern userPattern = Pattern.compile("user-*");
    final Pattern passwordPattern = Pattern.compile("password-*");

    final String username = user.getUsername();
    final String password = user.getPassword();

    return (userPattern.matcher(username).find() && passwordPattern.matcher(password).find())
        ? ClientResponse.create(HttpStatus.OK).build()
        : ClientResponse.create(HttpStatus.UNAUTHORIZED).build();
  }

  @Override
  public boolean acceptsUrl(@NotNull URL url) {
    return CreateTestData.URL_PATTERN.matcher(url.toString()).find();
  }

  @Override
  public @NotNull Duration getRefreshRate() {
    return Duration.ofDays(1_000);
  }

  @Override
  public Optional<URL> getDownloadURL(@NotNull URL url, @NotNull Set<HttpCookie> cookies) {
    return Optional.of(url);
  }

  @Override
  public UUID getPluginId() {
    return pluginId;
  }

  @Override
  public String getTitle() {
    return "Test file server plugin";
  }

  @Override
  public String getDescription() {
    return "Test file server plugin";
  }
}
