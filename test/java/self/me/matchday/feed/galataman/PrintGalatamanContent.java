/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.galataman;

import self.me.matchday.MatchDayTest;
import self.me.matchday.feed.Blogger;
import self.me.matchday.feed.BloggerPost;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

/**
 * Simple class to pull HTML content from Galataman posts.
 *
 * @author tomas
 */
public class PrintGalatamanContent 
{
    public static void main(String... args) throws IOException
    {
        // Get a known blog source
        Blogger blog = new Blogger(
                new URL(MatchDayTest.REMOTE_KNOWN_GOOD_JSON ),
                new GalatamanPostProcessor()
        );
        // File writer
        PrintWriter writer 
                = new PrintWriter(
                        "src/test/resources/content.txt"
                );
        
        // Store content of each entry
        blog
            .getEntries()
            .stream()
            .map(BloggerPost::getContent)
            .forEachOrdered(writer::println);
    }
}
