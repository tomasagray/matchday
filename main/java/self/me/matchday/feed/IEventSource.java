/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed;

import java.util.List;
import self.me.matchday.model.Event;

/**
 * Represents a source for a given Event. Events can have multiple Sources - for example, ESPN, BBC
 * and NBC all broadcast the same Match.
 */
public interface IEventSource {

  /**
   * Return the Event this source provides.
   *
   * @return An Event
   */
  Event getEvent();

  /**
   * Returns the file resources for this Event.
   *
   * @return An Event file resource
   */
  List<IEventFileSource> getEventFileSources();

}
