/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed.blogger.galataman;

/**
 * Indicates this post from the Galataman Blog was unable to be parsed.
 */
class GalatamanPostParseException extends RuntimeException {
  GalatamanPostParseException(String msg) {
    super(msg);
  }

  GalatamanPostParseException(String msg, Exception e) {
    super(msg, e);
  }
}
