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

/**
 * Application-wide abbreviation strategy. The current strategy is:
 *    Truncate the given String, removing all whitespace first. The user can supply a custom
 *    length for the abbreviation; otherwise the default length (3) will be used.
 */
public class Abbreviator {

  private static final int DEFAULT_LENGTH = 3;

  /**
   * Default method
   * @param str The String to be abbreviated.
   * @return The abbreviated String.
   */
  public static String abbreviate(final String str) {
    return abbreviate(str, DEFAULT_LENGTH);
  }

  /**
   * The worker method.
   * @param str The String to be abbreviated.
   * @return The abbreviated String.
   */
  public static String abbreviate(final String str, final int length) {
    String result = null;
    if (str != null) {
      final String noSpaces =
          str.replaceAll("\\s", "");
      // If we are shipped only whitespace, return null
      if (!noSpaces.equals("")) {
        if (noSpaces.length() < length) {
          result = noSpaces.toUpperCase();
        } else {
          result = noSpaces.substring(0, length).toUpperCase();
        }
      }
    }

    return result;
  }
}
