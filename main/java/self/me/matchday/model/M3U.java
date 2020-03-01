/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an extended M3U playlist, UTF-8 encoded (.m3u8), for a sporting event. The following
 * tags are hardcoded:
 *  - #EXT-X-VERSION:4
 *  - #EXT-X-ALLOW-CACHE:YES
 *  - #EXT-X-PLAYLIST-TYPE:EVENT
 *
 *  Records may be added to, but not deleted from the playlist.
 */
public abstract class M3U extends Playlist {

  @Override
  protected Playlist parseEvent(@NotNull EventSource eventSource) {
    final List<EventFileSource> eventFileSources = eventSource.getEventFileSources();
    eventFileSources.forEach( iEventFileSource -> {
//      final List<URL> urls = iEventFileSource.getUrls();
    });
    return null;
  }

  // Standard tags
  protected static final String HEADER = "#EXTM3U";
  protected static final String INF = "#EXTINF:";

}
