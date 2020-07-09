package self.me.matchday.plugin.blogger.parser;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.blogger.BloggerPost;
import self.me.matchday.plugin.blogger.BloggerPostParser;

public abstract class PostParserFactory {

  /**
   * Factory method to create the correct parser & sub-parsers.
   *
   * @param bloggerPost The Blogger Post to be parsed
   * @return A BloggerPostParser for this Post
   */
  public abstract BloggerPostParser createParser(@NotNull final BloggerPost bloggerPost);

}
