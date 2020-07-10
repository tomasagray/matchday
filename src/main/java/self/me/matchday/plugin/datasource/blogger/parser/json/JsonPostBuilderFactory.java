package self.me.matchday.plugin.datasource.blogger.parser.json;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public class JsonPostBuilderFactory {

  /**
   * Create a <b>JsonBloggerPostBuilder</b> instance from a chunk of JSON.
   *
   * @param postJson The JSON representing the <b>BloggerPost</b>
   * @return A JsonBloggerPostBuilder object from which can be extracted a BloggerPost.
   */
  public JsonBloggerPostBuilder createPostBuilder(@NotNull final JsonObject postJson) {
    return
        new JsonBloggerPostBuilder(postJson);
  }
}
