package self.me.matchday.feed.blogger;

import java.util.stream.Stream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.RemoteEventRepoFactory;
import self.me.matchday.model.Event;
import self.me.matchday.model.RemoteEventRepository;

public class BloggerRepoFactory extends RemoteEventRepoFactory {

  private final RemoteEventRepository remoteEventRepository;

  @Contract("_, _ -> new")
  public static @NotNull RemoteEventRepository createRepository(@NotNull final Blogger blog,
      @NotNull final PostEventSrcParserFactory postParserFactory) {

    return
        new BloggerRepoFactory(blog, postParserFactory)
            .getRepository();
  }

  /**
   * Create a Remote Event Repository from a Blogger blog
   *
   * @param blog          The Blogger blog
   * @param parserFactory Abstract factory for Post Parsers
   */
  private BloggerRepoFactory(@NotNull final Blogger blog,
      @NotNull final PostEventSrcParserFactory parserFactory) {

    // Map the Blogger's BloggerPost stream to an Event stream...
    final Stream<Event> eventStream =
        blog
            .getPosts()
            .map(bloggerPost -> {
              // ... using the PostParser implementation
              final BloggerPostEventSrcParser parser = parserFactory.createParser(bloggerPost);
              return parser.getEvent();
            });

    // Create new Remote Event Repository & return
    this.remoteEventRepository = new RemoteEventRepository(eventStream);
  }

  @Override
  protected RemoteEventRepository getRepository() {
    return this.remoteEventRepository;
  }
}
