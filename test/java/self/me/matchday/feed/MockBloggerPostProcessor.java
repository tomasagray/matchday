package self.me.matchday.feed;

import com.google.gson.JsonObject;

public final class MockBloggerPostProcessor implements IBloggerPostProcessor {
  public BloggerPost parse(JsonObject entry) {
    return new MockBloggerPost.MockBloggerBuilder(entry).build();
  }
}
