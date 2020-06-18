package self.me.matchday.feed.blogger.galataman;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.blogger.BloggerPost;
import self.me.matchday.feed.blogger.BloggerPostEventSrcParser;
import self.me.matchday.model.Event;
import self.me.matchday.feed.IEventFileSourceParser;
import self.me.matchday.feed.IEventParser;
import self.me.matchday.feed.IEventSourceParser;

public class GalatamanPostParser extends BloggerPostEventSrcParser {

  private final Event event;

  GalatamanPostParser(@NotNull final BloggerPost bloggerPost) {

    super(bloggerPost);
    // Instantiate parsers
    IEventParser eventParser = new GalatamanEventParser(bloggerPost.getTitle());
    IEventFileSourceParser fileSourceParser =
        new GManEventFileSourceParser(bloggerPost.getContent());
    // Get Event
    event = IEventSourceParser.createEvent(eventParser, fileSourceParser);
  }

  @Override
  public Event getEvent() {
    return this.event;
  }
}
