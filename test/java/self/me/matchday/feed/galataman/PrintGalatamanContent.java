/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.galataman;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import self.me.matchday.TestConstants;
import self.me.matchday.feed.Blogger;
import self.me.matchday.feed.BloggerPost;

/**
 * Simple class to pull HTML content from Galataman posts.
 *
 * @author tomas
 */
public class PrintGalatamanContent {
  public static void main(String... args) throws IOException {
    // Get a known blog source
    Blogger blog =
        new GalatamanBlog(
            new URL(TestConstants.REMOTE_KNOWN_GOOD_JSON), new GalatamanPostProcessor());
    // File writer
    PrintWriter writer = new PrintWriter("src/test/resources/content.txt");

    // Store content of each entry
    blog.getEntries().stream().map(BloggerPost::getContent).forEachOrdered(writer::println);
  }
}
