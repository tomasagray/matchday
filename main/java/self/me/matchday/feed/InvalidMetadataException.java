/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed;

/**
 * Indicates the metadata under consideration could not be understood by its parser, so the process
 * borked.
 */
public class InvalidMetadataException extends RuntimeException {

  private static final String DEFAULT_MSG = "Could not parse metadata!";

  public InvalidMetadataException() {
    super(DEFAULT_MSG);
  }

  public InvalidMetadataException(String msg) {
    super(msg);
  }

  public InvalidMetadataException(String msg, Exception e) {
    super(msg, e);
  }
}
