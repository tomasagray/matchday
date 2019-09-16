package self.me.matchday.feed.galataman;

class GalatamanPostParseException extends RuntimeException {
  GalatamanPostParseException(String msg) {
    super(msg);
  }

  GalatamanPostParseException(String msg, Exception e) {
    super(msg, e);
  }
}
