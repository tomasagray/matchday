/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.Galataman;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;
import self.me.matchday.MatchDayTest;
import self.me.matchday.feed.Blogger;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URL;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author tomas
 */
class GalatamanPostTest
{
    private static final String LOG_TAG = "GalatamanPostTest";


    private static Blogger currentBlog;
    private static Blogger knownGoodBlog;

    /**
     * Read test data, both a known good source and the latest edition.
     * Both should contain exactly 25 entries.
     *
     */
    @BeforeAll
    static void setup()
    {
        try {
            currentBlog = new Blogger(
                    new URL(MatchDayTest.REMOTE_CONTEMPORARY_JSON),
                    new GalatamanPostProcessor()
            );
            knownGoodBlog = new Blogger(
                    new URL(MatchDayTest.REMOTE_KNOWN_GOOD_JSON),
                    new GalatamanPostProcessor()
            );

        } catch (IOException e) {
            Log.d(LOG_TAG, "Could not read test data.");
        }
    }

    /**
     * Provide a Stream of GalatamanPosts
     *
     * @return Stream<Arguments> A stream of Arguments for test methods
     */
    private static Stream<Arguments> getArguments()
    {
        return
                Stream.concat(
                        currentBlog
                                .getEntries()
                                .stream()
//                                .map(post -> (GalatamanPost)post)
//                                .map(GalatamanPost::new)
                                .map(Arguments::of),
                        knownGoodBlog
                                .getEntries()
                                .stream()
//                                .map(post -> (GalatamanPost)post)
//                                .map(GalatamanPost::new)
                                .map(Arguments::of)
                );
    }


    @Tag("SOURCES")
    @DisplayName("Verify gets at least one source from each post")
    @ParameterizedTest(name="Testing: {index}; {0}")
    @MethodSource("getArguments")
    void getsAtLeastOneSourceFromEachPost(GalatamanPost gp)
    {
        try {
            assertTrue( gp.getSources().size() >= 1 );
            Log.d(LOG_TAG, "Found:\n" + gp);

        } catch (AssertionFailedError e) {
            String msg = "Does not contain at least one source:\n" + gp;
            throw new AssertionFailedError(msg, e);
        }
    }

    @Tag("SOURCES")
    @DisplayName("Ensure the code returns the correct # of sources")
    @Test
    void examineKnownGoodForCorrectSourceCount()
    {
        try {
            GalatamanPost gp = (GalatamanPost)knownGoodBlog.getEntries().get(0);
            assertEquals(4, gp.getSources().size() );

        } catch (AssertionFailedError e ) {
            String msg = "Link count test failed!:\n" + e.getMessage();
            throw new AssertionFailedError(msg, e);
        }
    }
}
