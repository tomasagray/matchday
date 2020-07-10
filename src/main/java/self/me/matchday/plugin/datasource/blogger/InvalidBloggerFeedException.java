/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.plugin.datasource.blogger;

/**
 * Indicates the parser encountered an irrecoverable error when examining this Blogger plugin.
 */
public class InvalidBloggerFeedException extends RuntimeException {

  public InvalidBloggerFeedException(String msg) {
    super(msg);
  }

  public InvalidBloggerFeedException(String msg, Exception e) {
    super(msg, e);
  }
}
