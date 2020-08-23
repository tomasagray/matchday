package self.me.matchday.plugin.datasource.zkfootball;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.Event;
import self.me.matchday.plugin.datasource.EventFileSourceParser;
import self.me.matchday.plugin.datasource.EventMetadataParser;
import self.me.matchday.plugin.datasource.EventSourceParser;
import self.me.matchday.plugin.datasource.blogger.BloggerPost;
import self.me.matchday.plugin.datasource.blogger.BloggerPostParser;

/**
 * Implementation of the BloggerPost parser, specific to the ZKFootball blog, found at:
 * https://zkfootballmatch.blogspot.com
 */
public class ZKFPostParser extends BloggerPostParser {

  private final Event event;

  ZKFPostParser(@NotNull final BloggerPost bloggerPost) {

    super(bloggerPost);
    // Instantiate parsers
    EventMetadataParser eventMetadataParser =
        new ZKFEventMetadataParser(bloggerPost.getTitle(), bloggerPost.getPublished());
    EventFileSourceParser fileSourceParser =
        new ZKFEventFileSourceParser(bloggerPost.getContent());
    // Get Event
    event = EventSourceParser.createEvent(eventMetadataParser, fileSourceParser);
  }

  @Override
  public Event getEvent() {
    return this.event;
  }
}