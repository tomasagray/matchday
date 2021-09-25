/*
 * Copyright (c) 2021.
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

package self.me.matchday.model.video;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class M3uRenderer implements VideoPlaylistRenderer {

  // Standard tags
  private static final String HEADER = "#EXTM3U";
  private static final String INF = "#EXTINF:";
  // M3U extended tags
  private static final String VERSION = "#EXT-X-VERSION:4";
  private static final String ALLOW_CACHE = "#EXT-X-ALLOW-CACHE:YES"; // allow clients to cache
  private static final String PLAYLIST_TYPE =
      "#EXT-X-PLAYLIST-TYPE:EVENT"; // allows playlist to be updated
  private static final String TARGET_DURATION =
      "#EXT-X-TARGETDURATION:"; // required; max duration in seconds
  private static final String MEDIA_SEQUENCE = "#EXT-X-MEDIA-SEQUENCE:"; // should begin at 0
  private static final String ENDLIST = "#EXT-X-ENDLIST"; // end of the playlist

  private final List<MediaSegment> mediaSegments = new ArrayList<>();
  @Getter private double targetDuration;

  /**
   * Add a segment to this playlist
   *
   * @param uri The URL for the segment (required)
   * @param title Title of the segment
   * @param duration The target duration of the segment
   */
  public void addMediaSegment(
      @NotNull final URI uri, @Nullable final String title, @Nullable Double duration) {

    final MediaSegment mediaSegment = new MediaSegment(uri, title, duration);
    // Create a new MediaSegment & add to collection
    this.mediaSegments.add(mediaSegment);
    // Update total duration
    targetDuration += (duration != null) ? duration : 0;
  }

  /**
   * Output a standards-conforming, UTF-8 encoded playlist (.m3u8 file). See:
   * https://tools.ietf.org/html/rfc8216
   *
   * @return The formatted playlist
   */
  @Override
  public String renderPlaylist() {

    // Container
    StringBuilder sb =
        new StringBuilder(HEADER)
            .append("\n")
            .append(PLAYLIST_TYPE)
            .append("\n")
            .append(VERSION)
            .append("\n")
            .append(ALLOW_CACHE)
            .append("\n")
            .append(TARGET_DURATION)
            .append(getTargetDuration())
            .append("\n")
            .append("\n")
            .append("\n")
            // Start at 0
            .append(MEDIA_SEQUENCE)
            .append(0)
            .append("\n");
    // Print each MediaSegment
    mediaSegments.forEach(
        segment -> {
          final String entry =
              String.format(
                  "%s%s, %s%n%s%n",
                  INF, segment.getDuration(), segment.getTitle(), segment.getUri());
          sb.append(entry);
        });
    sb.append(ENDLIST).append("\n");
    // Export playlist
    return sb.toString();
  }
}
