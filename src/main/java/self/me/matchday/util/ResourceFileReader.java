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

package self.me.matchday.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResourceFileReader {

  private static final Logger logger = LogManager.getLogger(ResourceFileReader.class);

  public static <T> @NotNull T getObjectFromProperties(
      @NotNull final Class<T> t_class, @NotNull final String filename, final String prefix)
      throws IOException, ReflectiveOperationException {

    // Instantiate object
    final T instance = t_class.getConstructor().newInstance();
    // Read properties file from disk
    final Map<String, String> properties = readPropertiesResource(filename);
    properties.forEach(
        (prop, val) -> {
          try {
            // Skip comments
            if (prop.startsWith("#")) {
              return;
            }
            // Parse property key
            if (prefix != null && prop.startsWith(prefix)) {
              // remove prefix from key
              prop = prop.replace(prefix + ".", "");
            }
            final String fieldName = dashToCamelCase(prop);
            // Get field
            final Field field = t_class.getDeclaredField(fieldName);
            // Save accessible
            final boolean accessible = field.canAccess(instance);
            // Set field
            field.setAccessible(true);
            final Long aLong = isLong(val);
            field.set(instance, Objects.requireNonNullElse(aLong, val));
            // Restore access state
            field.setAccessible(accessible);
          } catch (ReflectiveOperationException e) {
            final String msg =
                String.format(
                    "Property [%s] does not match a field in class %s; skipping...", prop, t_class);
            logger.error(msg, e);
          }
        });
    return instance;
  }

  public static @NotNull Map<String, String> readPropertiesResource(@NotNull final String filename)
      throws IOException {

    final String data = readTextResource(filename);
    return parsePropertiesData(data);
  }

  public static @NotNull String readTextResource(@NotNull final String filename)
      throws IOException {

    logger.info("Attempting to read data at: {}", filename);

    final ClassLoader classLoader = ResourceFileReader.class.getClassLoader();
    InputStream resourceAsStream = classLoader.getResourceAsStream(filename);
    if (resourceAsStream == null) {
      throw new FileNotFoundException("No resources found at: " + filename);
    }
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream))) {
      return reader.lines().collect(Collectors.joining("\n"));
    }
  }

  @NotNull
  private static Map<String, String> parsePropertiesData(@NotNull String data) {

    // Result container
    final Map<String, String> resources = new HashMap<>();
    final String[] lines = data.split("\n");
    String lastKey = null;
    // Examine file line by line
    for (String line : lines) {
      if (!(line.equals("")) && !line.startsWith("#")) {
        // Split line
        String[] split = line.split("=", 2);
        if (split.length > 0) {
          final String key = split[0];
          final String value = split.length == 2 ? split[1].replace(" \\", "").trim() : null;
          if (value == null && lastKey != null) {
            // Append to previous line
            String lastVal = resources.get(lastKey);
            resources.put(lastKey, lastVal + key);
            lastKey = null;
          } else {
            // Add to Map
            resources.put(key, value);
            lastKey = key;
          }
        }
      }
    }
    return resources;
  }

  private static @NotNull String dashToCamelCase(@NotNull final String str) {

    // Split on dashes
    final String[] words = str.split("-");
    final List<String> camelWords = new ArrayList<>();
    camelWords.add(words[0]);
    // Capitalize words
    for (int i = 1; i < words.length; ++i) {
      final String word = words[i];
      if (word != null && !("".equals(word))) {
        final String firstLetter = word.substring(0, 1).toUpperCase();
        camelWords.add(firstLetter + word.substring(1));
      }
    }
    // Collate & return
    return String.join("", camelWords);
  }

  private static @Nullable Long isLong(@NotNull final String str) {
    try {
      return Long.parseLong(str);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
