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

package self.me.matchday.plugin.datasource.blogger;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DataSourceValidator {

  @Value("${plugin.blogger.blogger-url-pattern}")
  private String bloggerUrlPattern;

  private Pattern urlPattern;

  void validateDataSourcePluginId(@NotNull UUID pluginId, @NotNull UUID dataSourcePluginId) {
    if (!pluginId.equals(dataSourcePluginId)) {
      throw new IllegalArgumentException("DataSource is not a Blogger (pluginId does not match)");
    }
  }

  void validateDataSourceUri(@NotNull URI uri) {
    final Matcher matcher = getUrlPattern().matcher(uri.toString());
    if (!matcher.find()) {
      throw new IllegalArgumentException("Given URL is not a Blogger URL: " + uri);
    }
  }

  Pattern getUrlPattern() {
    if (this.urlPattern == null) {
      this.urlPattern = Pattern.compile(bloggerUrlPattern);
    }
    return this.urlPattern;
  }
}
