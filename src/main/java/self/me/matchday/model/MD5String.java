/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
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
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Log;

/**
 * Class to generate MD5 hash Strings.
 *
 * @author tomas
 */
public class MD5String {

  private static final String LOG_TAG = "MD5String";
  private static final String DIGEST_ALGORITHM = "MD5";
  private static final int HASH_LEN = 32;

  public static String generate() {
    // Create a random String for MD5 computation
    final String generateData = Instant.now().toString() + Math.random();
    return fromData(generateData);
  }

  /**
   * Generate an MD5 hash from a supplied data String.
   * @param dataItems Data to be hashed.
   * @return A 32-character MD5 hash String
   */
  public static String fromData(@NotNull final String... dataItems) {

    // Collate data
    final String data = String.join("", dataItems);
    String hash = null;

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
