package self.me.matchday.feed;

import com.google.gson.JsonObject;

public interface IBloggerPostProcessor
{
    // Parse a chunk of JSON extracted from a Blogger
    // blog and return it as a post (type: BloggerPost)
    BloggerPost parse(JsonObject entry);
}
