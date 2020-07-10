package self.me.matchday.plugin.datasource.galataman;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.datasource.blogger.BloggerPost;
import self.me.matchday.plugin.datasource.blogger.BloggerPostParser;
import self.me.matchday.model.Event;
import self.me.matchday.plugin.datasource.EventFileSourceParser;
import self.me.matchday.plugin.datasource.EventParser;
import self.me.matchday.plugin.datasource.EventSourceParser;

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
