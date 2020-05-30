package self.me.matchday.feed.blogger.zkfootball;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.blogger.BloggerPost;
import self.me.matchday.feed.blogger.BloggerPostParser;
import self.me.matchday.model.Event;
import self.me.matchday.feed.IEventFileSourceParser;
import self.me.matchday.feed.IEventParser;
import self.me.matchday.feed.IEventSourceParser;

/**
 * Implementation of the BloggerPost parser, specific to the ZKFootball blog, found at:
 * https://zkfootballmatch.blogspot.com
 */
public class ZKFPostParser extends BloggerPostParser {

  private final Event event;

  ZKFPostParser(@NotNull final BloggerPost bloggerPost) {

    super(bloggerPost);
    // Instantiate parsers
    IEventParser eventParser =
        new ZKFEventParser(bloggerPost.getTitle(), bloggerPost.getPublished());
    IEventFileSourceParser fileSourceParser =
        new ZKFEventFileSourceParser(bloggerPost.getContent());
    // Get Event
    event = IEventSourceParser.createEvent(eventParser, fileSourceParser);
  }

  @Override
  public Event getEvent() {
    return this.event;
  }
}