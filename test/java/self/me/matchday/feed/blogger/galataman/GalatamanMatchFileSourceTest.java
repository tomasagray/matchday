/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.blogger.galataman;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;
import self.me.matchday.TestConstants;
import self.me.matchday.feed.blogger.Blogger;
import self.me.matchday.util.Log;

/** @author tomas */
class GalatamanMatchFileSourceTest {

  private static final String LOG_TAG = "GalatamanMatchFileSourceTest";

  private static Blogger currentBlog;
  private static Blogger knownGoodBlog;

  // Setup
  @BeforeAll
  static void setup() {
    try {
      currentBlog =
          new GalatamanBlog(
              new URL(TestConstants.REMOTE_CONTEMPORARY_JSON), new GalatamanPostProcessor());

//      knownGoodBlog =
//          new GalatamanBlog(
//              new URL(TestConstants.REMOTE_KNOWN_GOOD_JSON), new GalatamanPostProcessor());

    } catch (IOException e) {
      Log.e(LOG_TAG, "Could not read test data!", e);
    }
  }

  /**
   * Provide a Stream of GalatamanPosts
   *
   * @return Stream<Arguments> A stream of arguments for test methods
   */
  @NotNull
  private static Stream<Arguments> getArguments() {
    return //Stream.concat(
        currentBlog.getEntries().stream().map(Arguments::of);
//        knownGoodBlog.getEntries().stream().map(Arguments::of));
  }

  // Tests
  @DisplayName("Ensure every source has at least one link ")
  @ParameterizedTest(name = "Testing: {index}")
  @MethodSource("getArguments")
  void verifyGetsAtLeastOneLink(@NotNull GalatamanPost gp) {
    try {
      final int sourceCount = gp.getEventFileSources().size();
      Log.d(LOG_TAG, "Testing Post: " + gp.getTitle() + "\n\tContains " + sourceCount + " sources." );
      assertNotEquals(sourceCount, 0);

      gp.getEventFileSources()
          .forEach(
              source -> {
                int count = source.getEventFiles().size();
                assertTrue(count >= 1);
                Log.d(LOG_TAG, "Test passed, URL count: " + count);
              });

    } catch (AssertionFailedError e) {
      String msg = "Minimal link test failed on:\n" + gp + ", " + e.getMessage();
      throw new AssertionFailedError(msg, e);
    }
  }
}
