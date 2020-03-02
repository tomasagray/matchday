/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

/**
 * Represents an extended M3UPlaylist playlist, UTF-8 encoded (.m3u8), for a sporting event. The following
 * tags are hardcoded:
 *  - #EXT-X-VERSION:4
 *  - #EXT-X-ALLOW-CACHE:YES
 *  - #EXT-X-PLAYLIST-TYPE:EVENT
 */
public abstract class M3UPlaylist {

  // Standard tags
  protected static final String HEADER = "#EXTM3U";
  protected static final String INF = "#EXTINF:";

}
