/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.galataman;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author tomas
 */
class GalatamanMatchSourceTest
{
    private static final String LOG_TAG = "GalatamanMatchSourceTest";


    private static Blogger currentBlog;
    private static Blogger knownGoodBlog;

    // Setup
    // --------------------------------------------------------------------------------------------
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
            Log.e(LOG_TAG, "Could not read test data!", e);
        }
    }


    /**
     * Provide a Stream of GalatamanPosts
     *
     * @return Stream<Arguments> A stream of arguments for test methods
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

    // Tests
    // ------------------------------------------------------------------------------------------

    @Tag("LINKS")
    @DisplayName("Ensure every source has at least one link ")
    @ParameterizedTest(name="Testing: {index}")
    @MethodSource("getArguments")
    void verifyGetsAtLeastOneLink(GalatamanPost gp)
    {
        try {
            Log.d(LOG_TAG, "Testing Post: " + gp.getTitle() );

            gp.getSources().forEach(source -> {
                int count = source.getURLs().size();
                assertTrue(count >= 1);
                Log.d(LOG_TAG, "Test passed, URL count: " + count);
            });

        } catch (AssertionFailedError e) {
            String msg = "Minimal link test failed on:\n" + gp
                    + ", " + e.getMessage();
            throw new AssertionFailedError(msg, e);
        }
    }
}
