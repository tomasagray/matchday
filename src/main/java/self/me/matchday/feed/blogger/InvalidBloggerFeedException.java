/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.blogger;

/**
 * Indicates the parser encountered an irrecoverable error when examining this Blogger feed.
 */
public class InvalidBloggerFeedException extends RuntimeException {

  InvalidBloggerFeedException(String msg) {
    super(msg);
  }

  InvalidBloggerFeedException(String msg, Exception e) {
    super(msg, e);
  }
}
