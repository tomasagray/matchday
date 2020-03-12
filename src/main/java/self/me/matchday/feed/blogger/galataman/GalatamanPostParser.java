package self.me.matchday.feed.blogger.galataman;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.IEventFileSourceParser;
import self.me.matchday.feed.IEventParser;
import self.me.matchday.feed.IEventSourceParser;
import self.me.matchday.feed.blogger.BloggerPost;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventSource;

public class GalatamanPostParser implements IEventSourceParser {

  @Override
  public EventSource fromBloggerPost(@NotNull BloggerPost bloggerPost) {

    // Create data parsers
    final IEventParser eventDataParser = new GalatamanEventParser(bloggerPost.getTitle());
    final IEventFileSourceParser eventFileSourceParser =
        new GManEventFileSourceParser(bloggerPost.getContent());

    // Extract data from parsers
    final Event event = eventDataParser.getEvent();
    final List<EventFileSource> eventFileSources = eventFileSourceParser.getEventFileSources();

    // return EventSource
    return new EventSource(event, eventFileSources);
  }
}
