package self.me.matchday.plugin.blogger;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.EventSourceParser;

public abstract class BloggerPostParser implements EventSourceParser {

  protected final BloggerPost bloggerPost;

  protected BloggerPostParser(@NotNull final BloggerPost bloggerPost) {
    this.bloggerPost = bloggerPost;
  }

}
