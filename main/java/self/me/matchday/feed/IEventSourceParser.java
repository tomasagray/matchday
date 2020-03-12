package self.me.matchday.feed;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.blogger.BloggerPost;
import self.me.matchday.model.EventSource;

public interface IEventSourceParser {
  // todo: rename this? DECOUPLE!
  EventSource fromBloggerPost(@NotNull final BloggerPost bloggerPost);
}
