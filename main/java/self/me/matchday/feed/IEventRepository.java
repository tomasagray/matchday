/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed;

import java.util.stream.Stream;

/**
 * Represents a source of sporting Events. Provides a stream of Events for the end user to watch.
 */
public interface IEventRepository {

  /**
   * Return a Stream of all Events from this repository.
   *
   * @return A Stream<> of EventSource (Event sources)
   */
  Stream<EventSource> getEventSources();
}
