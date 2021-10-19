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

package self.me.matchday.plugin.datasource.blogger;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.video.VideoSourceMetadataPatternKit;

import javax.persistence.Entity;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@NoArgsConstructor
class BloggerDataSource extends DataSource {

  @Getter @Setter private SourceType sourceType;

  @Value("${plugin.blogger.blogger-url-pattern}")
  private String bloggerUrlPattern;

  public BloggerDataSource(
      @NotNull URI uri,
      @NotNull List<VideoSourceMetadataPatternKit> metadataPatterns,
      @NotNull UUID pluginId) {

    super(uri, metadataPatterns, pluginId);
    validateDataSourceUri(uri);
  }

  private void validateDataSourceUri(@NotNull URI uri) {

    final Pattern urlPattern = Pattern.compile(bloggerUrlPattern);
    final Matcher matcher = urlPattern.matcher(uri.toString());
    if (!matcher.find()) {
      throw new IllegalArgumentException("Given URL is not a Blogger URL: " + uri);
    }
  }

  public enum SourceType {
    HTML,
    JSON,
  }
}
