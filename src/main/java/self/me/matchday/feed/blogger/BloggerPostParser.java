package self.me.matchday.feed.blogger;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.IEventSourceParser;

public abstract class BloggerPostParser implements IEventSourceParser {

  /**
   * Default constructor
   *
   * @param bloggerPost A single Blogger post
   */
  protected BloggerPostParser(@NotNull final BloggerPost bloggerPost) {}
}
