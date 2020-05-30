package self.me.matchday.feed.blogger;

import org.jetbrains.annotations.NotNull;

public abstract class PostParserFactory {

  /**
   * Factory method to create the correct parser & sub-parsers.
   *
   * @param bloggerPost The Blogger Post to be parsed
   * @return A BloggerPostParser for this Post
   */
  public abstract BloggerPostParser createParser(@NotNull final BloggerPost bloggerPost);
}
