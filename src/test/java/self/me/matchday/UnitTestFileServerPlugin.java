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

@Component
public class UnitTestFileServerPlugin implements FileServerPlugin {

  private final UUID pluginId = UUID.fromString("bbd0bbac-116b-4809-a2f3-cc1ad6c93639");
  private final Pattern urlPattern =
      Pattern.compile("^http[s]?://commondatastorage.googleapis.com/[\\w-/.]+");

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
    return urlPattern.matcher(url.toString()).find();
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
    return "Unit test file server plugin";
  }

  @Override
  public String getDescription() {
    return "Unit test file server plugin";
  }
}
