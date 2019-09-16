package self.me.matchday.feed.galataman;

import com.google.gson.JsonObject;
import self.me.matchday.feed.IBloggerPostProcessor;

/**
 * Specialized class to parse JSON objects (from Gson) into Galataman BloggerPosts.
 */
public final class GalatamanPostProcessor implements IBloggerPostProcessor {
  public GalatamanPost parse(JsonObject entry) {
    return new GalatamanPost.GalatamanPostBuilder(entry).build();
  }
}
