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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.datasource.blogger.model.Blogger;

public interface BloggerParser {

  String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss[.nnn]z";

  /**
   * Fetch Blogger data from a remote URL and parse it into a Blogger instance
   *
   * @param url The location of the remote blog
   * @return A Blogger instance, created from String data read from supplied URL
   * @throws IOException If data cannot be read from remote source
   */
  default Blogger getBlogger(@NotNull final URL url) throws IOException {

    try (final InputStreamReader in = new InputStreamReader(url.openStream());
        final BufferedReader reader = new BufferedReader(in)) {
      final String data = reader.lines().collect(Collectors.joining("\n"));
      return getBlogger(data);
    }
  }

  /**
   * Parse text (HTML) data into a Blogger instance
   *
   * @param data The HTML data
   * @return A Blogger instance
   */
  Blogger getBlogger(@NotNull final String data);
}
