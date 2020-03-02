/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

// Simple playlist
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
public class VariantM3U extends M3UPlaylist {

  // M3UPlaylist extended tags
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
  @Id
  private String id;
  @ManyToOne(targetEntity = Event.class)
  @JoinColumn(name = "eventId")
  private Event event;
  @OneToMany(targetEntity = MediaSegment.class, cascade = CascadeType.ALL)
  private List<MediaSegment> mediaSegments = new ArrayList<>();
  private double targetDuration;
  private boolean finalized = true;

  public VariantM3U(@NotNull Event event, @NotNull List<EventFile> eventFiles) {

    // Generate playlist ID
    this.id = MD5String
        .fromData(event.getTitle() + "-" + eventFiles.stream().map(EventFile::toString)
            .collect(Collectors.joining("-")));

    // Save Event metadata
    this.event = event;

    // Add each event file as a listing in the playlist
    AtomicReference<Double> totalDuration = new AtomicReference<>(0.0d);
    eventFiles.forEach(
        eventFile -> {
          final String partTitle = event.getTitle() + " - " + eventFile.getTitle().toString();
          mediaSegments.add(
              new MediaSegment(eventFile.getInternalUrl(), eventFile.getDuration(), partTitle));
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
  public String toString() {

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
            .append(event.getDate())
            .append("\n")
            // Start at 0
            .append(MEDIA_SEQUENCE)
            .append(0)
            .append("\n");

    // Print each MediaSegment
    mediaSegments.forEach(sb::append);
    // Are we done with this playlist?
    if (isFinalized()) {
      sb.append(ENDLIST);
    }

    // Export playlist
    return sb.toString();
  }

  /**
   * Represents a single segment (record) in the playlist, which includes the URI of the media
   * resource, its duration in seconds and an optional title.
   */
  @Entity
  @NoArgsConstructor
  private static class MediaSegment {

    @Id
    @GeneratedValue
    private Long id;
    private URL url;
    private String title;
    private double duration;

    @Contract(pure = true)
    MediaSegment(@NotNull URL url, double duration) {
      this.url = url;
      this.duration = duration;
    }

    @Contract(pure = true)
    public MediaSegment(@NotNull URL url, double duration, String title) {
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
}
