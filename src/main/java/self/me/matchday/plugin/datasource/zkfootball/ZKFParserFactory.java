package self.me.matchday.plugin.datasource.zkfootball;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.datasource.blogger.BloggerPost;
import self.me.matchday.plugin.datasource.blogger.BloggerPostParser;
import self.me.matchday.plugin.datasource.blogger.parser.PostParserFactory;

public class ZKFParserFactory extends PostParserFactory {

  @Override
  public BloggerPostParser createParser(@NotNull BloggerPost bloggerPost) {
    return new ZKFPostParser(bloggerPost);
  }
}
