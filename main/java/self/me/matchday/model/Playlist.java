/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import org.jetbrains.annotations.NotNull;

public abstract class Playlist {

  protected abstract Playlist parseEvent(@NotNull EventSource event);
  // TODO: Eliminate?
  public Playlist fromEvent(@NotNull EventSource eventSource) {
    // Call implementation-specific factory method
    return this.parseEvent(eventSource);
  }

}
