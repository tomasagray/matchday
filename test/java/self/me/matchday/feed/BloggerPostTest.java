/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;
import self.me.matchday.MatchDayTest;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author tomas
 */
class BloggerPostTest
{
    private static final String LOG_TAG = "BloggerPostTest";


    private static Blogger currentBlog;
    private static Blogger knownGoodBlog;
    private static Blogger badBlog;
    
    /**
     * Read the test files
     * 
     */
    @BeforeAll
    static void setup()
    {
        try {
            // Read the JSON files & make Bloggers
            currentBlog = new Blogger(
                new URL(MatchDayTest.REMOTE_CONTEMPORARY_JSON),
                new BloggerPostProcessor()
            );
            knownGoodBlog = new Blogger(
                new URL(MatchDayTest.REMOTE_KNOWN_GOOD_JSON),
                new BloggerPostProcessor()
            );
            
        } catch(IOException e) {
            Log.d(LOG_TAG, "Could not open test file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get a Stream of BloggerPosts as Arguments.
     * Each sub-stream should contain 25 entries.
     * 
     */
    private static Stream<Arguments> getArguments()
    {
        return
            Stream.concat(
                currentBlog
                    .getEntries()
                    .stream()
                    .map(Arguments::of),
                knownGoodBlog
                    .getEntries()
                    .stream()
                    .map(Arguments::of)
            );
    }


    @Tag("FIELDS")
    @DisplayName("Check that Blogger IDs conform to pattern")
    @ParameterizedTest(name="Checking {index}: {0}")
    @MethodSource("getArguments")
    void verifySetsAllPostIDs(BloggerPost entry)
    {
        try {
            assertTrue(
                    entry
                            .getBloggerPostID()
                            .matches(
                                    "tag:blogger.com,\\d{4}:"
                                            + "blog-\\d*.post-\\d*"
                            )
            );

        } catch(AssertionFailedError e ) {
            String msg = "ID test failed on: " + entry.getBloggerPostID();
            throw new AssertionFailedError(msg, e);
        }
    }


    @Tag("FIELDS")
    @DisplayName("Check all post publication dates")
    @ParameterizedTest(name="Testing: {index}")
    @MethodSource("getArguments")
    void verifySetsAllPublishedDates(BloggerPost entry)
    {
        try {
            assertTrue(
                LocalDateTime.parse(
                    entry.getPublished().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
                ).toString().matches(
                    "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?"
                )
            );
        } catch(AssertionFailedError e) {
            String msg = "Published date test failed on:\n" + entry;
            throw new AssertionFailedError(msg, e);
        }
    }


    @Tag("FIELDS")
    @DisplayName("Check all post update dates")
    @ParameterizedTest(name="Testing: {index}")
    @MethodSource("getArguments")
    void verifySetsAllUpdatedDates(BloggerPost entry)
    {
        try {
            assertTrue(
                    LocalDateTime.parse(
                        entry.getLastUpdated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    ).toString().matches(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?"
                    )
            );
        } catch (AssertionFailedError e) {
            String msg = "Updated test failed on: " + entry;
            throw new AssertionFailedError(msg, e);
        }
    }


    @Tag("FIELDS")
    @DisplayName("Check all post categories are not null/empty")
    @ParameterizedTest(name="Testing: {index}")
    @MethodSource("getArguments")
    void verifySetsAllCategories(BloggerPost entry)
    {
        try {
            // Ensure no nulls/empties
            entry
                .getCategories()
                .forEach(category ->
                    assertNotEquals(
                        "", category
                    )
                );
        } catch(AssertionFailedError e ) {
            String msg = "Category not null test failed on: " + entry;
            throw new AssertionFailedError(msg, e);
        }
    }
    
    
    @Tag("FIELDS")
    @DisplayName("Check category size (should be at least 1)")
    @ParameterizedTest(name="Checking {index}: {0}")
    @MethodSource("getArguments")
    void verifyCategory(BloggerPost entry)
    {
        try{
            // Ensure post has at least one category
            // tag attached
            assertTrue( entry.getCategories().size() >= 1 );
            
        } catch(AssertionFailedError e) {
            String msg
                    = "Test failed on:\n" + entry + "\n"
                    + "Expected: >= 1"
                    + "; Actual: " + entry.getCategories().size();
            throw new AssertionFailedError(msg, e);
        }
    }
    
    
    @Tag("FIELDS")
    @DisplayName("Check that all titles are not empty/null")
    @ParameterizedTest(name="{index} => entries={0}")
    @MethodSource("getArguments")
    void verifySetsAllTitles(BloggerPost entry)
    {
        try {
            assertNotEquals(
                    "", entry.getTitle()
            );
        } catch (AssertionFailedError e) {
            String msg = "Title test failed on: " + entry;
            throw new AssertionFailedError(msg, e);
        }
    }
    
    
    @Tag("FIELDS")
    @DisplayName("Check all post content")
    @ParameterizedTest(name="Testing: {index}")
    @MethodSource("getArguments")
    void verifySetsAllContents(BloggerPost entry)
    {
        try {
            assertTrue(
                    entry.getContent().matches(
                            "(<\\w*)((\\s/>)|(.*</\\w*>))"
                    )
            );
        } catch (AssertionFailedError e) {
            String msg = "Content test failed on: " + entry;
            throw new AssertionFailedError(msg, e);
        }
    }
    
    
    @Tag("FIELDS")
    @DisplayName("Check all post links")
    @ParameterizedTest(name="Testing: {index}")
    @MethodSource("getArguments")
    void verifySetsAllLinks(BloggerPost entry)
    {
        try {
            assertTrue(
                    entry.getLink().matches(
                    "http://galatamanhdf.blogspot.com/\\d{4}/\\d{2}/[-,\\w]*.html"
                    )
            );
        } catch (AssertionFailedError e) {
            String msg = "Links test failed on: " + entry.getLink();
            throw new AssertionFailedError(msg, e);
        }
    }
}
