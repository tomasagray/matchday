/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed;

import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.blogger.Blogger;
import self.me.matchday.model.EventSource;

/**
 * Represents a source of sporting Events. Provides a stream of Events for the end user to watch.
 */
public abstract class RemoteEventRepository {

  private Stream<EventSource> eventSourceStream;

  public static RemoteEventRepository fromBlogger(@NotNull final Blogger blogger,
      @NotNull final IEventSourceParser eventSourceParser) {

    return
        new RemoteEventRepository() {}
          .setEventSourceStream(
              blogger
                  .getEntries()
                  .map(eventSourceParser::fromBloggerPost));
  }

  /**
   * Return a Stream of all Events from this repository.
   *
   * @return A Stream<> of EventSource (Event sources)
   */
  public Stream<EventSource> getEventSources() {
    return this.eventSourceStream;
  }

  RemoteEventRepository setEventSourceStream(@NotNull final Stream<EventSource> eventSourceStream) {
    this.eventSourceStream = eventSourceStream;
    return this;
  }
}
