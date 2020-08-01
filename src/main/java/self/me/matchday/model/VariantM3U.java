/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.net.URL;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.EventFile.EventPartIdentifier;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
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

  // Fields
  @Id
  private String id;
  @ManyToOne(targetEntity = Event.class)
  @JoinColumn(name = "eventId")
  private Event event;
  @OneToMany(targetEntity = MediaSegment.class, cascade = CascadeType.ALL)
  private Set<MediaSegment> mediaSegments = new TreeSet<>();
  private double targetDuration;
  private boolean finalized = true;

  // TODO: Rewrite this class - remove Event dependency, move to builder class
  public VariantM3U(@NotNull Event event, @NotNull Set<EventFile> eventFiles) {

    // Save Event metadata
    this.event = event;
    // Generate playlist ID
    this.id = MD5String
        .fromData(
            event + eventFiles
                .stream()
                .map(EventFile::toString)
                .collect(Collectors.joining("-"))
        );
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
    this.mediaSegments.add(new MediaSegment(event.getTitle(), eventFile));
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
  @NoArgsConstructor
  private static class MediaSegment implements Comparable<MediaSegment> {

    private URL url;
    private String eventTitle;
    private EventPartIdentifier partIdentifier;
    private double duration;

    @Contract(pure = true)
    public MediaSegment(@NotNull String eventTitle, @NotNull EventFile eventFile) {

      this.eventTitle = eventTitle;
      this.url = eventFile.getInternalUrl();
      this.duration = eventFile.getDuration();
      this.partIdentifier = eventFile.getTitle();
    }

    @Override
    public String toString() {

      return INF
          + duration
          + ","
          + eventTitle
          + " - "
          + partIdentifier
          + "\n"
          + url
          + "\n";
    }

    @Override
    public int compareTo(@NotNull MediaSegment test) {
      return this.partIdentifier.compareTo(test.partIdentifier);
    }
  }
}
