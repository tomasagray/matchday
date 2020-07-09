package self.me.matchday.plugin.galataman;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.blogger.BloggerPost;
import self.me.matchday.plugin.blogger.BloggerPostParser;
import self.me.matchday.plugin.blogger.parser.PostParserFactory;

public class GalatamanParserFactory extends PostParserFactory {

  @Override
  public BloggerPostParser createParser(@NotNull BloggerPost bloggerPost) {
    return new GalatamanPostParser(bloggerPost);
  }
}
