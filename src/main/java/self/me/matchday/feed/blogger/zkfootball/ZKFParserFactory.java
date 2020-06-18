package self.me.matchday.feed.blogger.zkfootball;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.blogger.BloggerPost;
import self.me.matchday.feed.blogger.BloggerPostEventSrcParser;
import self.me.matchday.feed.blogger.PostEventSrcParserFactory;

public class ZKFParserFactory extends PostEventSrcParserFactory {

  @Override
  public BloggerPostEventSrcParser createParser(@NotNull BloggerPost bloggerPost) {
    return new ZKFPostParser(bloggerPost);
  }
}
