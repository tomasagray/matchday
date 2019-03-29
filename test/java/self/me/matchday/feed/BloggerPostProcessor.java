package self.me.matchday.feed;

import com.google.gson.JsonObject;

public final class BloggerPostProcessor implements IBloggerPostProcessor
{
    public BloggerPost parse(JsonObject entry)
    {
        return new BloggerPost.BloggerPostBuilder(entry).build();
    }
}
