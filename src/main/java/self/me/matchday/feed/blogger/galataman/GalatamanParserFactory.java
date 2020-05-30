package self.me.matchday.feed.blogger.galataman;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.blogger.BloggerPost;
import self.me.matchday.feed.blogger.BloggerPostParser;
import self.me.matchday.feed.blogger.PostParserFactory;

public class GalatamanParserFactory extends PostParserFactory {

  @Override
  public BloggerPostParser createParser(@NotNull BloggerPost bloggerPost) {
    return new GalatamanPostParser(bloggerPost);
  }
}
