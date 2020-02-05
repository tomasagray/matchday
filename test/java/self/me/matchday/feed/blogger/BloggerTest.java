/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.blogger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import self.me.matchday.TestConstants;
import self.me.matchday.feed.MockBloggerPostProcessor;
import self.me.matchday.feed.blogger.galataman.GalatamanBlog;
import self.me.matchday.util.Log;

/**
 * Test suite for the Blogger class.
 *
 * @author tomas
 */
@TestInstance(Lifecycle.PER_CLASS)
class BloggerTest {

  private static final String LOG_TAG = "BloggerTest";

  private static final String IMPOSSIBLE = "This is an impossible link value.";
  private Blogger blog;

  // Setup
  /**
   * Read the known good example file.
   *
   * @throws IOException If the URL is invalid
   */
  @BeforeAll
  void setup() throws IOException {
    blog =
        new GalatamanBlog(
            new URL(TestConstants.REMOTE_KNOWN_GOOD_JSON), new MockBloggerPostProcessor());
  }

  // Tests
  /** Ensure the basic attributes of the Blog are read correctly. */
  @Test
  @Tag("GENERAL")
  @DisplayName("Verify Blogger class reads JSON data correctly.")
  void verifyHandlesExpectedJSONTest() {
    Log.d(LOG_TAG, "Testing Blog with ID: " + blog.getBlogId());

    // Perform tests
    assertEquals("GaLaTaMaN HD Football", blog.getTitle());
    Log.d(LOG_TAG, "Found Blog title: " + blog.getTitle());
    assertEquals("1.0", blog.getVersion());
    Log.d(LOG_TAG, "Found Blog version: " + blog.getVersion());
  }

  @Test
  @Tag("ENTRY")
  @DisplayName("Ensure Blog has at least one post (entry)")
  void verifyReadsAtLeastOneEntryTest() {
    int count = blog.getEntries().size();
    Log.d(LOG_TAG, "Blog has: " + count + " entries.");
    assertTrue(blog.getEntries().size() >= 1);
  }

  @Test
  @Tag("ENTRY")
  @DisplayName("Ensure reads correct # of entries from known source")
  void verifyReadsExactly25EntriesTest() {
    Log.d(LOG_TAG, "Expected: 25 entries, found: " + blog.getEntries().size());
    assertEquals(25, blog.getEntries().size());
  }

  /**
   * Reads a Blogger feed JSON file which has had the "link" portion removed. This should throw an
   * InvalidBloggerFeedException.
   */
  @Test
  @Tag("GENERAL")
  @DisplayName("Verify catches invalid JSON.")
  void verifyHandlesUnexpectedJSONTest() {
    Log.d(LOG_TAG, "Testing known corrupted data at: " + TestConstants.REMOTE_MISSING_DATA);

    try {
      // Read the file
      // Make sure Blogger.java handles the unexpected
      Blogger blg =
          new GalatamanBlog(
              new URL(TestConstants.REMOTE_MISSING_DATA), new MockBloggerPostProcessor());

      // Ensure the test fails if an exception is not thrown by the above code
      assert blg.getLink().equals(IMPOSSIBLE);
      Log.e(LOG_TAG, "The test failed. This should not have executed. 2 + 2 does not equal 5.");

    } catch (Exception e) {
      Log.d(
          LOG_TAG,
          "Caught exception: "
              + e.getClass().getName()
              + "\nExpecting: InvalidBloggerFeedException");
      // Test
      assertTrue(e instanceof InvalidBloggerFeedException);
    }
  }

  /** Reads a Blogger feed JSON file which has had all "entry" nodes removed. */
  @Test
  @Tag("ENTRY")
  @DisplayName("Verify responds correctly to Blog with no posts")
  void verifyRespondsToEmptySetTest() {
    Log.d(LOG_TAG, "Testing empty Blog at: " + TestConstants.EMPTY_SET);

    try {
      // Make a Blogger - should throw EmptyBloggerFeedException
      Blogger blg =
          new GalatamanBlog(new URL(TestConstants.EMPTY_SET), new MockBloggerPostProcessor());

      // Ensure test fails if no exception thrown. This should not execute
      assert blg.getLink().equals(IMPOSSIBLE);
      Log.e(LOG_TAG, "Test FAILED! This should NOT have executed.");

    } catch (Exception e) {
      Log.d(
          LOG_TAG,
          "Caught an exception: "
              + e.getClass().getName()
              + "\nExpecting: EmptyBloggerFeedException");
      // Should be the correct type of exception
      assertTrue(e instanceof EmptyBloggerFeedException);
      Log.d(LOG_TAG, "Test PASSED.");
    }
  }
}
