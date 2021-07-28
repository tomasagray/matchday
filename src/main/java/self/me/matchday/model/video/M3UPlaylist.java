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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
public class M3UPlaylist extends M3U {

  // M3U extended tags
  private static final String VERSION = "#EXT-X-VERSION:4";
  private static final String ALLOW_CACHE = "#EXT-X-ALLOW-CACHE:YES"; // allow clients to cache
  private static final String PLAYLIST_TYPE =
      "#EXT-X-PLAYLIST-TYPE:EVENT"; // allows playlist to be updated
  private static final String TARGET_DURATION =
      "#EXT-X-TARGETDURATION:"; // required; max duration in seconds
  private static final String PROGRAM_TIME =
      "#EXT-X-PROGRAM-DATE-TIME:"; // <YYYY-MM-DDThh:mm:ssZ>, ex: 2010-02-19T14:54:23.031+08:00
  private static final String MEDIA_SEQUENCE = "#EXT-X-MEDIA-SEQUENCE:"; // should begin at 0
  private static final String ENDLIST = "#EXT-X-ENDLIST"; // end of the playlist

  private List<MediaSegment> mediaSegments = new ArrayList<>();
  @Getter private double targetDuration;
  @Getter private boolean finalized = true;

  /**
   * Add a segment to this playlist
   *
   * @param url The URL for the segment (required)
   * @param title Title of the segment
   * @param duration The target duration of the segment
   */
  public M3UPlaylist addMediaSegment(
      @NotNull final URL url, @Nullable final String title, @Nullable Double duration) {

    final MediaSegment mediaSegment = new MediaSegment(url, title, duration);

    // Create a new MediaSegment & add to collection
    this.mediaSegments.add(mediaSegment);
    // Update total duration
    targetDuration += (duration != null) ? duration : 0;
    return this;
  }

  /**
   * Output a standards-conforming, UTF-8 encoded playlist (.m3u8 file). See:
   * https://tools.ietf.org/html/rfc8216
   *
   * @return The formatted playlist
   */
  @Override
  public String toString() {

    // Container
    StringBuilder builder =
        new StringBuilder(HEADER)
            .append("\n")
            // hardcoded fields
            .append(PLAYLIST_TYPE)
            .append("\n")
            .append(VERSION)
            .append("\n")
            .append(ALLOW_CACHE)
            .append("\n")
            // user set fields
            //            .append(TARGET_DURATION)
            //            .append(getTargetDuration())
            //            .append("\n")
            .append("\n")
            // Start at 0
            .append(MEDIA_SEQUENCE)
            .append(0)
            .append("\n");

    // Print each MediaSegment
    mediaSegments.forEach(builder::append);
    // Are we done with this playlist?
    if (isFinalized()) {
      builder.append(ENDLIST).append("\n");
    }
    // Export playlist
    return builder.toString();
  }

  /**
   * Format as a simple M3U playlist (no extended tags)
   *
   * @return A String of the playlist as a simple M3U
   */
  public String getSimplePlaylist() {

    return getMediaSegments().stream()
            .map(
                mediaSegment ->
                    String.format("# %s\n%s", mediaSegment.getTitle(), mediaSegment.getUrl()))
            .collect(Collectors.joining("\n"))
        + "\n";
  }

  // todo - make output separate from playlist structure
  public String getPlsPlaylist() {

    final StringBuilder sb = new StringBuilder("[playlist]\n\n");
    final List<MediaSegment> mediaSegments = getMediaSegments();
    final int segmentCount = mediaSegments.size();
    for (int i = 0; i < segmentCount; ++i) {
      final MediaSegment mediaSegment = mediaSegments.get(i);
      final URL url = mediaSegment.getUrl();
      final String title = mediaSegment.getTitle();
      final int fileNum = i + 1; // PLS must start with File1...
      sb.append("File").append(fileNum).append("=").append(url).append("\n");
      sb.append("Title").append(fileNum).append("=").append(title).append("\n");
    }
    sb.append("\n").append("NumberOfEntries=").append(segmentCount).append("\n");
    return sb.toString();
  }

  /**
   * Represents a single segment (record) in the playlist, which includes the URI of the media
   * resource, its duration in seconds and an optional title.
   */
  @Data
  static class MediaSegment {

    private final String title;
    private final URL url;
    private final Double duration;

    @Contract(pure = true)
    MediaSegment(
        @NotNull final URL url, @Nullable final String title, @Nullable final Double duration) {

      this.title = title;
      this.url = url;
      this.duration = duration;
    }

    @Override
    public String toString() {

      final String _duration = (duration != null) ? duration.toString() : "2000";
      final String _title = (title != null) ? title : "";

      return String.format("%s%s, %s\n%s\n", INF, _duration, _title, this.url);
    }
  }
}
