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

package net.tomasbot.matchday.model.video;

import java.util.Arrays;
import java.util.regex.Pattern;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Video resolution classes */
public enum Resolution {
  R_4k("4K", 3840, 2160),
  R_1080p("1080p", 1920, 1080),
  R_1080i("1080i", 1920, 1080),
  R_720p("720p", 1280, 720),
  R_576p("576p", 768, 576),
  R_SD("SD", 640, 480);

  // Fields
  private final String name;
  private final Pattern pattern;
  @Getter private final int width;
  @Getter private final int height;

  @Contract(pure = true)
  Resolution(@NotNull String name, int width, int height) {
    this.name = name;
    this.pattern = Pattern.compile(".*" + name + ".*");
    this.width = width;
    this.height = height;
  }

  /**
   * Factory method to return an enumerated video resolution from a given String.
   *
   * @param str The String to be converted.
   * @return The Resolution value, or <b>null</b> if the given String does not match an enumerated
   *     value.
   */
  @Nullable
  @Contract(pure = true)
  public static Resolution fromString(@NotNull String str) {
    return Arrays.stream(Resolution.values())
        .filter(
            resolution -> {
              final boolean nameMatches = resolution.pattern.matcher(str).matches();
              final boolean heightMatches =
                  String.format("%sp", resolution.getHeight()).equals(str);
              return nameMatches || heightMatches;
            })
        .findFirst()
        .orElse(null);
  }

  @Override
  public String toString() {
    return this.name;
  }
}
