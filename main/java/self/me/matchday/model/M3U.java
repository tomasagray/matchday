/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.EventFileSource;
import self.me.matchday.feed.EventSource;

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

  // TODO: Wrapper class for VariantM3U and SimpleM3U (hide)?
  // Standard tags
  protected static final String HEADER = "#EXTM3U";
  protected static final String INF = "#EXTINF:"; // required; format: #EXTINF:<duration>,<title>
  protected static final String PLAYLIST_EXT = "m3u8";

}
