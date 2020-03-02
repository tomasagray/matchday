/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.blogger;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;
import self.me.matchday.TestConstants;
import self.me.matchday.feed.MockBloggerPostProcessor;
import self.me.matchday.feed.blogger.galataman.GalatamanBlog;
import self.me.matchday.util.Log;

/** @author tomas */
class BloggerPostTest {

  private static final String LOG_TAG = "BloggerPostTest";

  private static Blogger currentBlog;
  private static Blogger knownGoodBlog;

  /** Read the test files */
  @BeforeAll
  static void setup() {
    try {
      // Read the JSON files & make Bloggers
      currentBlog =
          new GalatamanBlog(
              new URL(TestConstants.REMOTE_CONTEMPORARY_JSON), new MockBloggerPostProcessor());
      knownGoodBlog =
          new GalatamanBlog(
              new URL(TestConstants.REMOTE_KNOWN_GOOD_JSON), new MockBloggerPostProcessor());

    } catch (IOException e) {
      Log.d(LOG_TAG, "Could not open test file: " + e.getMessage(), e);
    }
  }

  /** Get a Stream of BloggerPosts as Arguments. Each sub-stream should contain 25 entries. */
  @NotNull
  private static Stream<Arguments> getArguments() {
    return Stream.concat(
        currentBlog.getEntries().stream().map(Arguments::of),
        knownGoodBlog.getEntries().stream().map(Arguments::of));
  }

  @Tag("FIELDS")
  @DisplayName("Check Blogger IDs conform to pattern")
  @ParameterizedTest(name = "Checking {index}: {0}")
  @MethodSource("getArguments")
  void verifySetsAllPostIDs(@NotNull BloggerPost entry) {
    try {
      Log.d(LOG_TAG, "Testing Post ID: " + entry.getBloggerPostID() + "...");
      assertTrue(
          entry.getBloggerPostID().matches("tag:blogger.com,\\d{4}:" + "blog-\\d*.post-\\d*"));

      Log.d(LOG_TAG, "Passed.");

    } catch (AssertionFailedError e) {
      String msg = "ID test failed on: " + entry.getBloggerPostID();
      throw new AssertionFailedError(msg, e);
    }
  }

  @Tag("FIELDS")
  @DisplayName("Check all post publication dates are valid dates")
  @ParameterizedTest(name = "Testing: {index}")
  @MethodSource("getArguments")
  void verifySetsAllPublishedDates(@NotNull BloggerPost entry) {
    try {
      Log.d(LOG_TAG, "Publication date: " + entry.getPublished() + "...");

      assertTrue(
          LocalDateTime.parse(
                  entry.getPublished().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                  DateTimeFormatter.ISO_LOCAL_DATE_TIME)
              .toString()
              .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?"));

      Log.d(LOG_TAG, "Passed.");

    } catch (AssertionFailedError e) {
      String msg = "Published date test failed on:\n" + entry;
      throw new AssertionFailedError(msg, e);
    }
  }

  @Tag("FIELDS")
  @DisplayName("Check all post update dates are valid dates")
  @ParameterizedTest(name = "Testing: {index}")
  @MethodSource("getArguments")
  void verifySetsAllUpdatedDates(@NotNull BloggerPost entry) {
    try {
      Log.d(LOG_TAG, "Update date: " + entry.getLastUpdated() + "...");

      assertTrue(
          LocalDateTime.parse(
                  entry.getLastUpdated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                  DateTimeFormatter.ISO_LOCAL_DATE_TIME)
              .toString()
              .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?"));

      Log.d(LOG_TAG, "Passed.");

    } catch (AssertionFailedError e) {
      String msg = "Updated test failed on: " + entry;
      throw new AssertionFailedError(msg, e);
    }
  }

  @Tag("FIELDS")
  @DisplayName("Check all post categories are not null/empty, and there is at least 1")
  @ParameterizedTest(name = "Testing: {index}")
  @MethodSource("getArguments")
  void verifySetsAllCategories(@NotNull BloggerPost entry) {
    try {
      int catCount = entry.getCategories().size();
      Log.d(LOG_TAG, "Post has: " + catCount + " categories.");

      // TESTS:*******************************
      // Ensure we have at least one category set
      assertTrue(catCount >= 1);
      // Ensure no nulls/empties
      entry
          .getCategories()
          .forEach(
              category -> {
                Log.d(LOG_TAG, "Testing category: " + category);
                assertNotEquals("", category);
              });

      Log.d(LOG_TAG, "All tests passed.");

    } catch (AssertionFailedError e) {
      String msg = "Category not null test failed on: " + entry;
      throw new AssertionFailedError(msg, e);
    }
  }

  @Tag("FIELDS")
  @DisplayName("Check all titles match pattern")
  @ParameterizedTest(name = "{index} => entries={0}")
  @MethodSource("getArguments")
  void verifySetsAllTitles(@NotNull BloggerPost entry) {
    try {
      final String title = entry.getTitle();
      final Pattern titlePattern = Pattern.compile("(([\\w\\d/ ])+ - )+");
      final Matcher matcher = titlePattern.matcher(title);

      Log.d(LOG_TAG, "Checking title: " + entry.getTitle());

      boolean found = matcher.find();
      assert found; // TEST
      Log.d(LOG_TAG, "Passed.");

    } catch (AssertionFailedError e) {
      String msg = "Title test failed on: " + entry;
      throw new AssertionFailedError(msg, e);
    }
  }

  @Tag("FIELDS")
  @DisplayName("Check all post content contains HTML")
  @ParameterizedTest(name = "Testing: {index}")
  @MethodSource("getArguments")
  void verifySetsAllContents(@NotNull BloggerPost entry) {
    try {
      Log.d(
          LOG_TAG,
          "Testing content:\n-------------------------------------------------------\n"
              + entry.getContent());
      assertTrue(entry.getContent().matches("(<\\w*)((\\s/>)|(.*</\\w*>))"));
      Log.d(LOG_TAG, "\n\nPassed.");

    } catch (AssertionFailedError e) {
      String msg = "Content test failed on: " + entry;
      throw new AssertionFailedError(msg, e);
    }
  }

  @Tag("FIELDS")
  @DisplayName("Check all post links match are valid URLs")
  @ParameterizedTest(name = "Testing: {index}")
  @MethodSource("getArguments")
  void verifySetsAllLinks(@NotNull BloggerPost entry) {

    final String TEST_PATTERN = "[^:]*://.*.blogspot.com/\\d{4}/\\d{2}/[-,\\w]*.html";

    try {
      Log.d(LOG_TAG, "Testing link: " + entry.getLink());
      assertTrue( entry.getLink().matches(TEST_PATTERN) );
      Log.d(LOG_TAG, "Passed.");

    } catch (AssertionFailedError e) {
      String msg = "Links test failed on: " + entry.getLink();
      throw new AssertionFailedError(msg, e);
    }
  }
}
