/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.IEventFileSource;
import self.me.matchday.feed.IEventSource;

/**
 * Represents an extended M3U playlist, UTF-8 encoded (.m3u8), for a sporting event. The following
 * tags are hardcoded:
 *  - #EXT-X-VERSION:4
 *  - #EXT-X-ALLOW-CACHE:YES
 *  - #EXT-X-PLAYLIST-TYPE:EVENT
 *
 *  Records may be added to, but not deleted from the playlist.
 */
public class M3U extends Playlist {

  @Override
  protected Playlist parseEvent(@NotNull IEventSource eventSource) {
    final List<IEventFileSource> eventFileSources = eventSource.getEventFileSources();
    eventFileSources.forEach( iEventFileSource -> {
      final List<URL> urls = iEventFileSource.getUrls();
    });
    return null;
  }

  /**
   * Represents a single segment (record) in the playlist, which includes the URI of the media
   * resource, its duration in seconds and an optional title.
   */
  public static class MediaSegment {

    private final URL url;
    private final float duration;
    private String title;

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

    public URL getUrl() {
      return url;
    }

    public float getDuration() {
      return duration;
    }

    public String getTitle() {
      return title;
    }
  }

  // Static members
  // Standard tags
  private static final String HEADER = "#EXTM3U";
  private static final String INF = "#EXTINF:"; // required; format: #EXTINF:<duration>,<title>
  // M3U extended tags
  private static final String VERSION = "#EXT-X-VERSION:4";
  private static final String ALLOW_CACHE = "#EXT-X-ALLOW-CACHE:YES"; // allow clients to cache
  private static final String PLAYLIST_TYPE = "#EXT-X-PLAYLIST-TYPE:EVENT"; // allows playlist to be updated
  private static final String TARGET_DURATION = "#EXT-X-TARGETDURATION:"; // required; max duration in seconds
  private static final String PROGRAM_TIME = "#EXT-X-PROGRAM-DATE-TIME:"; // <YYYY-MM-DDThh:mm:ssZ>, ex: 2010-02-19T14:54:23.031+08:00
  private static final String MEDIA = "#EXT-X-MEDIA:";  // for linking multiple versions
  private static final String STREAM_INF = "#EXT-X-STREAM-INF";
  private static final String MEDIA_SEQUENCE = "#EXT-X-MEDIA-SEQUENCE:";  // should begin at 0
  private static final String ENDLIST = "#EXT-X-ENDLIST"; // end of the playlist

  // Fields
  private float targetDuration;
  private LocalDateTime programDateTime;
  private final List<MediaSegment> mediaSegments = new ArrayList<>();
  private boolean finalized = false;

  // Setters
  /**
   * Set the maximum duration of any given segment.
   * @param duration The duration of the longest segment, in seconds.
   */
  public void setTargetDuration(float duration) {
    this.targetDuration = duration;
  }

  /**
   * Set the datetime of the event. The date/time representation is ISO/IEC 8601:2004 [ISO_8601] and
   * SHOULD indicate a time zone:
   *  #EXT-X-PROGRAM-DATE-TIME:<YYYY-MM-DDThh:mm:ssZ>
   * For example:
   *  #EXT-X-PROGRAM-DATE-TIME:2010-02-19T14:54:23.031+08:00
   *
   * @param programDateTime The datetime of the Match
   */
  public void setProgramTime(LocalDateTime programDateTime) {
    this.programDateTime = programDateTime;
  }

  /**
   * Add a media segment URI to the list of segments. These will be added to any outputted playlist
   * in the order in which they were added.
   *
   * @param segment The URL of the media resource.
   */
  public void addMediaSegment(MediaSegment segment) {
    this.mediaSegments.add(segment);
  }

  /**
   * Set whether any more segments will be added to the playlist.
   * @param finalized Will any more segments be added?
   */
  public void setFinalized(boolean finalized) {
    this.finalized = finalized;
  }

  // Getters
  /**
   * Returns the longest allowable segment duration, rounded UP to the nearest integer.
   *
   * @return Maximum media segment duration
   */
  public int getTargetDuration() {
    return (int) Math.ceil(this.targetDuration / 100.0f);
  }

  public LocalDateTime getProgramDateTime() {
    return programDateTime;
  }

  boolean isFinalized() {
    return finalized;
  }

  /**
   * Output a standards-conforming, UTF-8 encoded playlist (.m3u8 file).
   * See:
   *  https://tools.ietf.org/html/rfc8216
   * @return The formatted playlist
   */
  public String getPlaylistAsString() {
    // Container
    StringBuilder sb = new StringBuilder();

    // Print header
    sb.append(HEADER)
        // hardcoded fields
        .append(PLAYLIST_TYPE).append("\n")
        .append(VERSION).append("\n")
        .append(ALLOW_CACHE).append("\n")
        // user set fields
        .append(TARGET_DURATION)
        .append(getTargetDuration()).append("\n")
        .append(PROGRAM_TIME)
        .append(getProgramDateTime()).append("\n")
        // Start at 0
        .append(MEDIA_SEQUENCE).append(0).append("\n");

    // Print each media segment
    for(MediaSegment segment : mediaSegments) {
      // Print tag & duration
      sb.append(INF)
          .append(segment.duration)
          .append(",");
      if (segment.title != null) {
        sb.append(segment.title);
      }
      // Print URL
      sb.append("\n")
          .append(segment.url)
          .append("\n");
    }

    // Are we done with this playlist?
    if(isFinalized()) {
      sb.append(ENDLIST);
    }

    // Export playlist
    return sb.toString();
  }
}
