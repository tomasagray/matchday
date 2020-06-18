package self.me.matchday.feed.blogger;

import org.jetbrains.annotations.NotNull;

public abstract class PostEventSrcParserFactory {

  /**
   * Factory method to create the correct parser & sub-parsers.
   *
   * @param bloggerPost The Blogger Post to be parsed
   * @return A BloggerPostEventSrcParser for this Post
   */
  public abstract BloggerPostEventSrcParser createParser(@NotNull final BloggerPost bloggerPost);
}
