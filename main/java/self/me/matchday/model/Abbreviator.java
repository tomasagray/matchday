/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

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
  static String abbreviate(final String str) {
    return abbreviate(str, DEFAULT_LENGTH);
  }

  /**
   * The worker method.
   * @param str The String to be abbreviated.
   * @return The abbreviated String.
   */
  static String abbreviate(final String str, final int length) {
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
