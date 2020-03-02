/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.io;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import self.me.matchday.util.Log;

/** @author tomas */
class TextFileReaderTest {

  private static final String LOG_TAG = "TextFileReaderTest";
  private static List<String> invalidUrls;

  @BeforeAll
  static void setup() {
    invalidUrls =
        List.of(
            "http://ww.nothing.com",
            "http://&&&www.google.com/",
            "http;www.google.com/",
            "This, of course, is not a valid URL",
            "http//",
            "http//www.google.com/");
  }

  /**
   * Provide the bad URLs as a Stream of Arguments for parameterized tests.
   * @return The bad URLs as a Stream.
   */
  private static Stream<Arguments> getBadUrls() {
    return invalidUrls.stream().map(Arguments::of);
  }

  @DisplayName("Ensure IOExceptions are thrown from invalid URLs")
  @ParameterizedTest(name = "Testing url {index}: {0}")
  @MethodSource("getBadUrls")
  void testBadUrls(@NotNull String badUrl) {
    // Ensure each generates an exception
    try {
      Log.d(LOG_TAG, "Testing: " + badUrl);
      // Attempt to read non-existent data
      String nothing = TextFileReader.readRemote(new URL(badUrl));
      // This should not execute
      Log.d(LOG_TAG, "This should not have executed.\n" + nothing);
      assert false;

    } catch (Exception e) {
      Log.d(LOG_TAG, "Caught exception type: " + e.getClass().getName());
      // Make sure each one throws an IO exception
      Assertions.assertTrue(e instanceof IOException);
    }
  }

}
