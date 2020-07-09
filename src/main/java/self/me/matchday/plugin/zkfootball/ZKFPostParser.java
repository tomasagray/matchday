package self.me.matchday.plugin.zkfootball;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.blogger.BloggerPost;
import self.me.matchday.plugin.blogger.BloggerPostParser;
import self.me.matchday.model.Event;
import self.me.matchday.plugin.EventFileSourceParser;
import self.me.matchday.plugin.EventParser;
import self.me.matchday.plugin.EventSourceParser;

/**
 * Implementation of the BloggerPost parser, specific to the ZKFootball blog, found at:
 * https://zkfootballmatch.blogspot.com
 */
public class ZKFPostParser extends BloggerPostParser {

  private final Event event;

  ZKFPostParser(@NotNull final BloggerPost bloggerPost) {

    super(bloggerPost);
    // Instantiate parsers
    EventParser eventParser =
        new ZKFEventParser(bloggerPost.getTitle(), bloggerPost.getPublished());
    EventFileSourceParser fileSourceParser =
        new ZKFEventFileSourceParser(bloggerPost.getContent());
    // Get Event
    event = EventSourceParser.createEvent(eventParser, fileSourceParser);
  }

  @Override
  public Event getEvent() {
    return this.event;
  }
}