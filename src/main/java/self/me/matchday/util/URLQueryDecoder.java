/*
 * Copyright (c) 2020.
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

package self.me.matchday.util;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class URLQueryDecoder {

  /**
   * Decode a URL query String into key/value pairs
   *
   * @param urlString A String representing a URL
   * @return A Map<> of key/value pairs, or empty map
   */
  public static Map<String, List<String>> decode(@NotNull final String urlString) {

    // Return an empty Set<> if any problems
    try {

      // Decode URL
      final String decodedUrl = URLDecoder.decode(urlString, StandardCharsets.UTF_8);
      final URL url = new URL(decodedUrl);
      // If the query is blank, return an empty set
      if (isNullOrEmpty(url.getQuery())) {
        throw new IllegalArgumentException();
      }

      // Split URL query, map to key/value pairs & return
      return
          Arrays
              .stream(url.getQuery().split("&"))
              .map(URLQueryDecoder::splitQueryParameter)
              .collect(
                  Collectors
                      .groupingBy(
                          SimpleImmutableEntry::getKey,
                          LinkedHashMap::new,
                          mapping(Map.Entry::getValue, toList())));

    } catch (MalformedURLException | RuntimeException e) {
      // A problem was encountered; return an empty map
      return Collections.emptyMap();
    }
  }

  /**
   * Split each query parameter into key/value pairs
   *
   * @param param The URL query parameter String
   * @return A key/value pair
   */
  private static @NotNull SimpleImmutableEntry<String, String> splitQueryParameter(
      @NotNull final String param) {

    final int idx = param.indexOf("=");
    final String key = idx > 0 ? param.substring(0, idx) : param;
    final String value = idx > 0 && param.length() > idx + 1 ? param.substring(idx + 1) : null;
    return new SimpleImmutableEntry<>(key, value);
  }

  /**
   * Determines whether the given String is null or empty or contains only whitespace.
   * @param string The String to be tested
   * @return True / false
   */
  private static boolean isNullOrEmpty(final String string) {
    return
        (string == null) || ("".equals(string.trim()));
  }
}
