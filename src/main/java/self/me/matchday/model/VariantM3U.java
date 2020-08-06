/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.EventFile.EventPartIdentifier;

@Data
@EqualsAndHashCode(callSuper = true)
public class VariantM3U extends M3UPlaylist {

  // M3UPlaylist extended tags
  private static final String VERSION = "#EXT-X-VERSION:4";
  private static final String ALLOW_CACHE = "#EXT-X-ALLOW-CACHE:YES"; // allow clients to cache
  private static final String PLAYLIST_TYPE =
      "#EXT-X-PLAYLIST-TYPE:VOD"; // allows playlist to be updated
  private static final String TARGET_DURATION =
      "#EXT-X-TARGETDURATION:"; // required; max duration in seconds
  private static final String PROGRAM_TIME =
      "#EXT-X-PROGRAM-DATE-TIME:"; // <YYYY-MM-DDThh:mm:ssZ>, ex: 2010-02-19T14:54:23.031+08:00
  private static final String MEDIA_SEQUENCE = "#EXT-X-MEDIA-SEQUENCE:"; // should begin at 0
  private static final String ENDLIST = "#EXT-X-ENDLIST"; // end of the playlist

  private List<MediaSegment> mediaSegments = new ArrayList<>();
  private double targetDuration;
  private boolean finalized = true;

  public VariantM3U(@NotNull Collection<EventFile> eventFiles) {

    // Add each event file as a URL in the playlist
    eventFiles.forEach(this::createMediaSegment);
  }

  /**
   * Create a new MediaSegment (playlist URL entry) and update the target duration (total playlist
   * time).
   *
   * @param eventFile The EventFile which represents the playlist entry
   */
  private void createMediaSegment(@NotNull EventFile eventFile) {

    // Create a new MediaSegment & add to collection
    this.mediaSegments.add(new MediaSegment(eventFile));
    // Update total playlist duration
    targetDuration += eventFile.getDuration();
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
            .append(TARGET_DURATION)
            .append(getTargetDuration())
            .append("\n")
            .append("\n")
            // Start at 0
            .append(MEDIA_SEQUENCE)
            .append(0)
            .append("\n");

    // Print each MediaSegment
    mediaSegments.forEach(builder::append);
    // Are we done with this playlist?
    if (isFinalized()) {
      builder
          .append(ENDLIST)
          .append("\n");
    }
    // Export playlist
    return builder.toString();
  }

  /**
   * Represents a single segment (record) in the playlist, which includes the URI of the media
   * resource, its duration in seconds and an optional title.
   */
  private static class MediaSegment {

    private final String title;
    private final URL url;
    private final double duration;

    @Contract(pure = true)
    public MediaSegment(@NotNull final EventFile eventFile) {

      final EventPartIdentifier title = eventFile.getTitle();
      this.title = (title != null) ? title.toString() : null;
      this.url = eventFile.getInternalUrl();
      this.duration = eventFile.getDuration();
    }

    @Override
    public String toString() {

      return INF
          + duration
          + ","
          + title
          + "\n"
          + url
          + "\n";
    }
  }
}
