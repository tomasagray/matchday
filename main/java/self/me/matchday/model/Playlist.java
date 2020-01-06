/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.IEventSource;

public abstract class Playlist {

  protected abstract Playlist parseEvent(@NotNull IEventSource event);

  public Playlist fromEvent(@NotNull IEventSource eventSource) {
    // Call implementation-specific factory method
    return this.parseEvent(eventSource);
  }

}
