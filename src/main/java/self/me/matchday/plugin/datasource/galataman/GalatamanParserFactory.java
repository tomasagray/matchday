package self.me.matchday.plugin.datasource.galataman;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.datasource.blogger.BloggerPost;
import self.me.matchday.plugin.datasource.blogger.BloggerPostParser;
import self.me.matchday.plugin.datasource.blogger.parser.PostParserFactory;

public class GalatamanParserFactory extends PostParserFactory {

  @Override
  public BloggerPostParser createParser(@NotNull BloggerPost bloggerPost) {
    return new GalatamanPostParser(bloggerPost);
  }
}
