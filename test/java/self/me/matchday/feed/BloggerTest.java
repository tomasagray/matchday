/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import self.me.matchday.MatchDayTest;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test suite for the Blogger class.
 *
 * @author tomas
 */
@TestInstance(Lifecycle.PER_CLASS)
class BloggerTest 
{
    private static final String LOG_TAG = "BloggerTest";

    private Blogger blog;

    // Setup
    // ---------------------------------------------------------------------------------------------
    /**
     * Read the known good example file.
     * 
     * @throws IOException If the URL is invalid
     */
    @BeforeAll
    void setup() throws IOException
    {
        blog = new Blogger(
                new URL( MatchDayTest.REMOTE_KNOWN_GOOD_JSON),
                new BloggerPostProcessor()
        );
    }


    // Tests
    // ------------------------------------------------------------------------------------------

    /**
     * Ensure the basic attributes of the Blog are read
     * correctly.
     *
     */
    @Test
    @Tag("GENERAL")
    @DisplayName("Verify Blogger class reads JSON data correctly.")
    void verifyHandlesExpectedJSONTest()
    {
        Log.d(LOG_TAG, "Testing Blog with ID: " + blog.getBlogId() );

        // Perform tests
        assertEquals(
                "GaLaTaMaN HD Football",
                blog.getTitle()
        );
        Log.d(LOG_TAG, "Found Blog title: " + blog.getTitle());
        assertEquals(
                "1.0",
                blog.getVersion()
        );
        Log.d(LOG_TAG, "Found Blog version: " + blog.getVersion() );
    }
    
    @Test
    @Tag("ENTRY")
    @DisplayName("Ensuring Blog has at least one post (entry)")
    void verifyReadsAtLeastOneEntryTest()
    {
        int count = blog.getEntries().size();
        Log.d(LOG_TAG, "Blog has: " + count + " entries.");
        assertTrue( blog.getEntries().size() >= 1 );
    }
    
    @Test
    @Tag("ENTRY")
    @DisplayName("Ensure reads correct # of entries from known source")
    void verifyReadsExactly25EntriesTest()
    {
        Log.d(LOG_TAG, "Expected: 25 entries, found: " + blog.getEntries().size());
        assertEquals( 25, blog.getEntries().size() );
    }
    
    
    /**
     * Reads a Blogger feed JSON file which has had the "link" portion
     * removed. This should throw an InvalidBloggerFeedException.
     * 
     */
    @Test
    @Tag("GENERAL")
    @DisplayName("Verify catches invalid JSON.")
    void verifyHandlesUnexpectedJSONTest()
    {
        Log.d(LOG_TAG, "Testing known corrupted data at: " + MatchDayTest.REMOTE_MISSING_DATA);

        try {
            // Read the file
            // Make sure Blogger.java handles the unexpected
            Blogger blg = new Blogger(
                    new URL(MatchDayTest.REMOTE_MISSING_DATA),
                    new BloggerPostProcessor()
            );
            // Try to use the object; this should NOT execute
            Log.e(
                    LOG_TAG,
                    "This should NOT have printed!\nBlog: " + blg
            );

        } catch(Exception e) {
            Log.d( LOG_TAG,"Caught exception: " + e.getClass().getCanonicalName() );

            assertTrue(
                    e instanceof InvalidBloggerFeedException
            );
        }
    }
    
    /**
     * Reads a Blogger feed JSON file which has had all "entry" nodes removed.
     * 
     */
    @Test
    @Tag("ENTRY")
    @DisplayName("Verify responds correctly to Blog with no posts")
    void verifyRespondsToEmptySetTest()
    {
        Log.d(LOG_TAG, "Testing empty Blog at: " + MatchDayTest.EMPTY_SET );

        try {
            // Make a Blogger - should throw EmptyBloggerFeedException
            Blogger blg = new Blogger(
                    new URL(MatchDayTest.EMPTY_SET),
                    new BloggerPostProcessor()
            );

            // This should not execute
            Log.e(
                    LOG_TAG,
                    "This should NOT be printed: "
                    + blg.getEntries().size()
            );

        } catch(Exception e) {
            Log.d(LOG_TAG, "Caught an exception: " + e.getMessage());
            // Should be the correct type of exception
            assertTrue(
                 e instanceof EmptyBloggerFeedException
            );

        }
    }
}
