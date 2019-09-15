package self.me.matchday.model;

public class InvalidMetadataException extends RuntimeException {
  private static final String message = "Could not parse metadata into a key/value pair!";

  InvalidMetadataException(String msg) {
    super(message + msg);
  }

  InvalidMetadataException(String msg, Exception e) {
    super(message + msg, e);
  }
}
