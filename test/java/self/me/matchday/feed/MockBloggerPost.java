/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed;

import com.google.gson.JsonObject;
import self.me.matchday.feed.blogger.BloggerPost;

public class MockBloggerPost extends BloggerPost {


  protected MockBloggerPost(BloggerPostBuilder builder) {
    super(builder);
  }

  public static class MockBloggerBuilder extends BloggerPostBuilder {

    public MockBloggerBuilder(JsonObject bloggerPost) {
      super(bloggerPost);
    }

    @Override
    public BloggerPost build() {
      // Parse mandatory fields
      parsePostID();
      parsePublished();
      parseTitle();
      parseLink();
      parseContent();
      // Parse optional fields
      parseLastUpdated();
      parseCategories();
      // Construct a fully-formed BloggerPost object
      return new MockBloggerPost(this);
    }
  }
}
