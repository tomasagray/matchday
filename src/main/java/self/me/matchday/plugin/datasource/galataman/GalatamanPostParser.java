package self.me.matchday.plugin.datasource.galataman;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.Event;
import self.me.matchday.plugin.datasource.EventFileSourceParser;
import self.me.matchday.plugin.datasource.EventMetadataParser;
import self.me.matchday.plugin.datasource.EventSourceParser;
import self.me.matchday.plugin.datasource.blogger.BloggerPost;
import self.me.matchday.plugin.datasource.blogger.BloggerPostParser;

public class GalatamanPostParser extends BloggerPostParser {

  private final Event event;

  GalatamanPostParser(@NotNull final BloggerPost bloggerPost) {

    super(bloggerPost);
    // Instantiate parsers
    EventMetadataParser eventMetadataParser =
        new GalatamanEventMetadataParser(bloggerPost.getTitle());
    EventFileSourceParser fileSourceParser =
        new GManEventFileSourceParser(bloggerPost.getContent());
    // Get Event
    this.event = EventSourceParser.createEvent(eventMetadataParser, fileSourceParser);
  }

  @Override
  public Event getEvent() {
    return this.event;
  }
}
