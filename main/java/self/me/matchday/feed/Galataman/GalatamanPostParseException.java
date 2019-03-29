package self.me.matchday.feed.Galataman;

class GalatamanPostParseException extends RuntimeException
{
    GalatamanPostParseException(String msg)
    {
        super(msg);
    }
    GalatamanPostParseException(String msg, Exception e)
    {
        super(msg, e);
    }
}
