/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed;

/** @author tomas */
public class EmptyBloggerFeedException extends RuntimeException {
  @Override
  public String getMessage() {
    return "This feed contains no entries, or is an invalid Blogger feed.";
  }
}
