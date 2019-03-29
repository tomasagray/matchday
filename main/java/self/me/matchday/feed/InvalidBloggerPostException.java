package self.me.matchday.feed;

class InvalidBloggerPostException extends RuntimeException
{
    InvalidBloggerPostException(String msg, RuntimeException e)
    {
        super(msg, e);
    }
    InvalidBloggerPostException(String msg) {
        super(msg);
    }
}
