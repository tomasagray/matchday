/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed;

import com.google.gson.JsonObject;
import self.me.matchday.feed.blogger.BloggerPost;
import self.me.matchday.feed.blogger.IBloggerPostProcessor;

public final class MockBloggerPostProcessor implements IBloggerPostProcessor {
  public BloggerPost parse(JsonObject entry) {
    return new MockBloggerPost.MockBloggerBuilder(entry).build();
  }
}
