package self.me.matchday.plugin.galataman;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.blogger.BloggerPost;
import self.me.matchday.plugin.blogger.BloggerPostParser;
import self.me.matchday.model.Event;
import self.me.matchday.plugin.EventFileSourceParser;
import self.me.matchday.plugin.EventParser;
import self.me.matchday.plugin.EventSourceParser;

public class GalatamanPostParser extends BloggerPostParser {

  private final Event event;

  GalatamanPostParser(@NotNull final BloggerPost bloggerPost) {

    super(bloggerPost);
    // Instantiate parsers
    EventParser eventParser = new GalatamanEventParser(bloggerPost.getTitle());
    EventFileSourceParser fileSourceParser =
        new GManEventFileSourceParser(bloggerPost.getContent());
    // Get Event
    event = EventSourceParser.createEvent(eventParser, fileSourceParser);
  }

  @Override
  public Event getEvent() {
    return this.event;
  }
}
