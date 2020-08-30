/*
 * Copyright (c) 2020.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.model;

/**
 * Represents an extended M3UPlaylist playlist, UTF-8 encoded (.m3u8), for a sporting event. The following
 * tags are hardcoded:
 *  - #EXT-X-VERSION:4
 *  - #EXT-X-ALLOW-CACHE:YES
 *  - #EXT-X-PLAYLIST-TYPE:EVENT .
 */
public abstract class M3UPlaylist {

  // Standard tags
  protected static final String HEADER = "#EXTM3U";
  protected static final String INF = "#EXTINF:";

}
