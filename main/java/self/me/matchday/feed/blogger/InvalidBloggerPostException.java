/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed.blogger;

/**
 * Indicates this particular Blogger post could not be understood by the Post parser.
 */
public class InvalidBloggerPostException extends RuntimeException {
  public InvalidBloggerPostException(String msg, RuntimeException e) {
    super(msg, e);
  }

  public InvalidBloggerPostException(String msg) {
    super(msg);
  }
}
