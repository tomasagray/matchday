package self.me.matchday.feed.blogger.json;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public class JsonPostBuilderFactory {

  /**
   * Create a <b>BloggerJsonPostBuilder</b> instance from a chunk of JSON.
   *
   * @param postJson The JSON representing the <b>BloggerPost</b>
   * @return A BloggerJsonPostBuilder object from which can be extracted a BloggerPost.
   */
  public BloggerJsonPostBuilder createPostBuilder(@NotNull final JsonObject postJson) {
    return
        new BloggerJsonPostBuilder(postJson);
  }
}
