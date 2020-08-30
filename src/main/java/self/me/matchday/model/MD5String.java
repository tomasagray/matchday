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

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Log;

/**
 * Class to generate MD5 hash Strings.
 *
 * @author tomas
 */
@SuppressWarnings("ConstantConditions")
public class MD5String {

  private static final String LOG_TAG = "MD5String";
  private static final String DIGEST_ALGORITHM = "MD5";
  private static final int HASH_LEN = 32;

  private final String hash;

  public static String generate() {
    // Create a random String for MD5 computation
    return
        new MD5String(Instant.now().toString(), Math.random()).toString();
  }

  public static String fromData(@NotNull final Object... data) {
    return
        new MD5String(data).toString();
  }

  @Override
  public String toString() {
    return this.hash;
  }

  private MD5String(@NotNull final Object... data) {
    this.hash = generateHash(data);
  }

  /**
   * Generate an MD5 hash from a supplied data String.
   *
   * @param dataItems Data to be hashed.
   * @return A 32-character MD5 hash String
   */
  private String generateHash(@NotNull final Object... dataItems) {

    // Result container
    String hash = null;

    // Collate data
    final List<String> strings =
        Arrays
            .stream(dataItems)
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.toList());
    final String data = String.join("", strings);

    try {
      MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
      byte[] messageDigest = md.digest(data.getBytes());

      // Convert byte array into signum representation
      BigInteger no = new BigInteger(1, messageDigest);

      // Convert message digest into hex value
      StringBuilder hashText = new StringBuilder(no.toString(16));
      while (hashText.length() < HASH_LEN) {
        hashText.insert(0, "0");
      }

      hash = hashText.toString();

    } catch (NoSuchAlgorithmException e) {
      Log.e(LOG_TAG, "ERROR: NoSuchAlgorithmException thrown for MD5String of:\n" + data, e);
    }

    return hash;
  }
}
