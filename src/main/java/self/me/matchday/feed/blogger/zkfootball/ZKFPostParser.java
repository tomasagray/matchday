package self.me.matchday.feed.blogger.zkfootball;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.IEventFileSourceParser;
import self.me.matchday.feed.IEventParser;
import self.me.matchday.feed.IEventSourceParser;
import self.me.matchday.feed.blogger.BloggerPost;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventSource;

/**
 * Implementation of the BloggerPost parser, specific to the ZKFootball blog, found at:
 * https://zkfootballmatch.blogspot.com
 */
public class ZKFPostParser implements IEventSourceParser {

  @Override
  public EventSource fromBloggerPost(@NotNull final BloggerPost bloggerPost) {

    // Create data parsers
    final IEventParser eventParser = new ZKFEventParser(bloggerPost.getTitle(),
        bloggerPost.getPublished());
    final IEventFileSourceParser eventFileSourceParser = new ZKFEventFileSourceParser(
        bloggerPost.getContent());
    // Get data
    final Event event = eventParser.getEvent();
    final List<EventFileSource> eventFileSources = eventFileSourceParser.getEventFileSources();
    // Create an EventSource & return
    return new EventSource(event, eventFileSources);
  }
}
