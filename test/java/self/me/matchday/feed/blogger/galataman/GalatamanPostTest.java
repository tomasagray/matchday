/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.blogger.galataman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;
import self.me.matchday.TestConstants;
import self.me.matchday.feed.blogger.Blogger;
import self.me.matchday.util.Log;

/** @author tomas */
class GalatamanPostTest {
  private static final String LOG_TAG = "GalatamanPostTest";

  private static Blogger currentBlog;
  private static Blogger knownGoodBlog;

  /**
   * Read test data, both a known good source and the latest edition. Both should contain exactly 25
   * entries.
   */
  @BeforeAll
  static void setup() {
    try {
      currentBlog =
          new GalatamanBlog(
              new URL(TestConstants.REMOTE_CONTEMPORARY_JSON), new GalatamanPostProcessor());
      knownGoodBlog =
          new GalatamanBlog(
              new URL(TestConstants.REMOTE_KNOWN_GOOD_JSON), new GalatamanPostProcessor());

    } catch (IOException e) {
      Log.d(LOG_TAG, "Could not read test data.");
    }
  }

  /**
   * Provide a Stream of GalatamanPosts
   *
   * @return Stream<Arguments> A stream of Arguments for test methods
   */
  @NotNull
  private static Stream<Arguments> getArguments() {
    return Stream.concat(
        currentBlog.getEntries().stream()
            .map(Arguments::of),
        knownGoodBlog.getEntries().stream()
            .map(Arguments::of));
  }

  @Tag("SOURCES")
  @DisplayName("Verify gets at least one source from each post")
  @ParameterizedTest(name = "Testing: {index}; {0}")
  @MethodSource("getArguments")
  void getsAtLeastOneSourceFromEachPost(@NotNull GalatamanPost gp) {
    final int MIN_SOURCE_COUNT = 1;

    try {
      final int sourceCount = gp.getEventFileSources().size();
      Log.d(LOG_TAG, "Testing post at URL: " + gp.getLink());
      Log.d(LOG_TAG, "Number of  sources found: " + sourceCount);

      // Test
      assertTrue(sourceCount >= MIN_SOURCE_COUNT);

      // Print results
      gp.getEventFileSources().forEach(System.out::println);

    } catch (AssertionFailedError e) {
      String msg = "TEST FAILED! Does not contain at least one source:\n" + gp;
      throw new AssertionFailedError(msg, e);
    }
  }

  @Tag("SOURCES")
  @DisplayName("Ensure the code returns the correct # of sources")
  @Test
  void examineKnownGoodForCorrectSourceCount() {
    final int TEST_ENTRY = 1;
    final int EXPECTED_ENTRY_COUNT = 4;
    try {
      Log.d(LOG_TAG, "Expecting " + EXPECTED_ENTRY_COUNT + " sources");
      GalatamanPost gp = (GalatamanPost) knownGoodBlog.getEntries().get(TEST_ENTRY);
      Log.d(LOG_TAG, "Testing entry at URL: " + gp.getLink());
      assertEquals(EXPECTED_ENTRY_COUNT, gp.getEventFileSources().size());

    } catch (AssertionFailedError e) {
      String msg = "Link count test failed!:\n" + e.getMessage();
      throw new AssertionFailedError(msg, e);
    }
  }
}
