package self.me.matchday.plugin.zkfootball;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.blogger.BloggerPost;
import self.me.matchday.plugin.blogger.BloggerPostParser;
import self.me.matchday.plugin.blogger.parser.PostParserFactory;

public class ZKFParserFactory extends PostParserFactory {

  @Override
  public BloggerPostParser createParser(@NotNull BloggerPost bloggerPost) {
    return new ZKFPostParser(bloggerPost);
  }
}
