/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.blogger;

/**
 * Indicates the Blogger contains no posts, or the parser was not capable of finding any.
 */
public class EmptyBloggerFeedException extends RuntimeException {

  @Override
  public String getMessage() {
    return "This feed contains no entries, or is an invalid Blogger feed.";
  }
}
