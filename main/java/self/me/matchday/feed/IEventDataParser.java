/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed;

import self.me.matchday.model.Event;

/**
 * Should parse event data from the data stream on which it operates, and return a properly
 * constructed <i>? extends Event</i> object.
 */
public interface IEventDataParser {

  /**
   * Return an Event subclass instantiation.
   *
   * @return An Event object.
   */
  Event getEvent();
}
