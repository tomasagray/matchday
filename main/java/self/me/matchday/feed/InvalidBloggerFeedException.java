/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed;

/**
 *
 * @author tomas
 */
class InvalidBloggerFeedException extends RuntimeException
{
    InvalidBloggerFeedException(String msg, Exception e)
    {
        super(msg, e);
    }
}
