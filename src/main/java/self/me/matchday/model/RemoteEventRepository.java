/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.blogger.Blogger;
import self.me.matchday.feed.blogger.BloggerPostParser;
import self.me.matchday.feed.blogger.PostParserFactory;

/**
 * Represents a source of sporting Events. Provides a stream of Events for the end user to watch.
 */
public final class RemoteEventRepository {

  private final Stream<Event> eventStream;

  public RemoteEventRepository(@NotNull final Stream<Event> eventStream) {
    this.eventStream = eventStream;
  }

  /**
   * Return a Stream of all Events from this repository.
   *
   * @return A Stream<> of Events
   */
  public Stream<Event> getEventStream() {
    return this.eventStream;
  }
}
