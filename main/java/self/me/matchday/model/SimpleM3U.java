/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.EventFileSource;

// Simple playlist
@EqualsAndHashCode(callSuper = true)
@Data
public class SimpleM3U extends M3U {

  // ex:
  // /matches/match/{matchId}/play/{srcId}/{variantId}.m3u8
  private static final String SIMPLE_URI_FORMAT = "%s/%s.%s";
  static String generateSimplePlaylistUrl(@NotNull final EventFileSource eventFileSource) {
    final Long id = eventFileSource.getEventFileSrcId();

    // resolution-channel-lang
    final String VARIANT_ID_FORMAT = "%s-%s-%s";
    // Variant ID
    final String resolution = eventFileSource.getResolution().getName();
    final String channel =
        eventFileSource.getChannel().replaceAll(" ", "_").toLowerCase();
    final String primaryLanguage =
        // get the first lang if there is one
        (eventFileSource.getLanguages().size() > 0)
            ? eventFileSource.getLanguages().get(0).toLowerCase()
            : "null";
    final String variantId = String.format(VARIANT_ID_FORMAT, resolution, channel, primaryLanguage);

    return String.format(SIMPLE_URI_FORMAT, id, variantId, PLAYLIST_EXT);
  }

  /**
   * Represents a single segment (record) in the playlist, which includes the URI of the media
   * resource, its duration in seconds and an optional title.
   */
  private static class MediaSegment {

    private final URL url;
    private String title;
    private final float duration;

    @Contract(pure = true)
    MediaSegment(@NotNull URL url, float duration) {
      this.url = url;
      this.duration = duration;
    }

    @Contract(pure = true)
    public MediaSegment(@NotNull URL url, float duration, String title) {
      this(url, duration);
      this.title = title;
    }

    @Override
    public String toString() {
      // Print tag & duration
      final StringBuilder sb = new StringBuilder(INF).append(this.duration).append(",");
      if (this.title != null) {
        sb.append(this.title);
      }
      // Print URL
      sb.append("\n").append(this.url).append("\n");

      return sb.toString();
    }
  }

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

  // Fields
  private final String eventTitle;
  private final LocalDateTime dateTime;
  private final List<MediaSegment> mediaSegments = new ArrayList<>();
  private final float targetDuration;
  private boolean finalized = true;

  public SimpleM3U(@NotNull Event event, @NotNull List<EventFile> eventFiles) {
    // Event metadata
    dateTime = event.getDate();
    eventTitle = event.getTitle();

    // Add each event file as a listing in the playlist
    AtomicReference<Float> totalDuration = new AtomicReference<>((float) 0);
    eventFiles.forEach(
        eventFile -> {
          final String partTitle = eventTitle + " - " + eventFile.getTitle().toString();
          mediaSegments.add(
              new MediaSegment(eventFile.getUri(), eventFile.getDuration(), partTitle));
          // Add to total playlist length
          totalDuration.updateAndGet(currTot -> currTot + eventFile.getDuration());
        });
    targetDuration = totalDuration.get();
  }

  /**
   * Output a standards-conforming, UTF-8 encoded playlist (.m3u8 file). See:
   * https://tools.ietf.org/html/rfc8216
   *
   * @return The formatted playlist
   */
  @Override
  public String getPlaylistAsString() {

    // Container
    StringBuilder sb =
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
            .append(PROGRAM_TIME)
            .append(getDateTime())
            .append("\n")
            // Start at 0
            .append(MEDIA_SEQUENCE)
            .append(0)
            .append("\n");

    // Print each media segment
    mediaSegments.forEach(sb::append);
    // Are we done with this playlist?
    if (isFinalized()) {
      sb.append(ENDLIST);
    }

    // Export playlist
    return sb.toString();
  }
}
