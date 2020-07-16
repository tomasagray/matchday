package self.me.matchday.plugin.datasource;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFileSource;

public interface EventSourceParser {

  /**
   * Return a properly initialized Event
   *
   * @return An Event
   */
  Event getEvent();

  /**
   * Event factory method.
   *
   * @param eventMetadataParser Parser containing Event metadata (Competition, Teams, etc.)
   * @param fileSourceParser File source data parser
   * @return A complete Event with File Sources
   */
  static @NotNull Event createEvent(@NotNull final EventMetadataParser eventMetadataParser,
      @NotNull final EventFileSourceParser fileSourceParser) {

    // Extract data from parsers
    final Event event = eventMetadataParser.getEvent();
    final List<EventFileSource> eventFileSources = fileSourceParser.getEventFileSources();

    // Add file sources to Event & return
    event.addFileSources(eventFileSources);
    return event;
  }
}