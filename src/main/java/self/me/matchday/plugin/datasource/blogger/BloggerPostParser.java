package self.me.matchday.plugin.datasource.blogger;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.datasource.EventSourceParser;

public abstract class BloggerPostParser implements EventSourceParser {

  protected final BloggerPost bloggerPost;

  protected BloggerPostParser(@NotNull final BloggerPost bloggerPost) {
    this.bloggerPost = bloggerPost;
  }

}
