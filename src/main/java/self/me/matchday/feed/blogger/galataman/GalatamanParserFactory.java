package self.me.matchday.feed.blogger.galataman;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.blogger.BloggerPost;
import self.me.matchday.feed.blogger.BloggerPostEventSrcParser;
import self.me.matchday.feed.blogger.PostEventSrcParserFactory;

public class GalatamanParserFactory extends PostEventSrcParserFactory {

  @Override
  public BloggerPostEventSrcParser createParser(@NotNull BloggerPost bloggerPost) {
    return new GalatamanPostParser(bloggerPost);
  }
}
