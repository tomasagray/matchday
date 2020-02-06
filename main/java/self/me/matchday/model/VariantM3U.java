/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.EventFileSource;
import self.me.matchday.feed.EventFileSource.Resolution;
import self.me.matchday.feed.EventSource;

// Variant playlist
public final class VariantM3U extends M3U {

  // M3U extended tags
  private static final String MEDIA_LINK = "#EXT-X-MEDIA:";  // for linking multiple versions
  private static final String MEDIA_TYPE = "TYPE=VIDEO";
  private static final String GROUP_ID_TAG = "GROUP-ID=";
  private static final String NAME_TAG = "NAME=";
  private static final String DEFAULT_TAG = "DEFAULT=";
  private static final String URI_TAG = "URI=";
  private static final String STREAM_INF = "#EXT-X-STREAM-INF:";
  private static final String BANDWIDTH_TAG = "BANDWIDTH=";
  private static final String VIDEO_TAG = "VIDEO=";
  private static final String RESOLUTION_TAG = "RESOLUTION=";

  private static class SimplePlaylistEntry {

    private static final int BITRATE_INDEX = 0;
    // Fields
    private final String simplePlaylistUrl;
    private final Resolution resolution;
    private final List<String> languages;
    private final long bitrate;

    SimplePlaylistEntry(EventFileSource eventFileSource) {
      simplePlaylistUrl = SimpleM3U.generateSimplePlaylistUrl(eventFileSource);
      resolution = eventFileSource.getResolution();
      languages = eventFileSource.getLanguages();
      bitrate = parseBitrate(eventFileSource.getVideoData().get(BITRATE_INDEX));
    }

    String getSimplePlaylistUrl() {
      return this.simplePlaylistUrl;
    }

    @NotNull
    private static Long parseBitrate(@NotNull String videoBitrate) {
      final double CONVERSION_FACTOR = 1_000_000d;
      // Convert to a double, then convert Mbps -> bps
      double bitrate =
          Double.parseDouble(videoBitrate.substring(0, videoBitrate.indexOf(" ")))
              * CONVERSION_FACTOR;
      return (long) bitrate;
    }

    @Override
    public String toString() {
      return MEDIA_LINK
          + MEDIA_TYPE
          + ","
          + GROUP_ID_TAG
          + "\""
          + this.resolution.getName()
          + "\","
          + NAME_TAG
          + "\""
          + String.join("/", this.languages)
          + "\","
          // todo: add default status
          + URI_TAG
          + "\""
          + this.simplePlaylistUrl
          + "\"";
    }
  }

  final Map<Resolution, List<SimplePlaylistEntry>> simplePlaylistEntries;

  public VariantM3U(@NotNull final EventSource eventSource) {
    // Organize file sources by video resolution
    simplePlaylistEntries =
        eventSource.getEventFileSources().stream()
            .collect(
                groupingBy(
                    EventFileSource::getResolution,
                    TreeMap::new, // sort by resolution (map key)
                    mapping(SimplePlaylistEntry::new, toList())));
  }

  /**
   * Output a standards-conforming, UTF-8 encoded playlist (.m3u8 file).
   * See:
   *  https://tools.ietf.org/html/rfc8216
   * @return The formatted playlist
   */
  @NotNull
  @Override
  public String getPlaylistAsString() {
    final StringBuilder stringBuilder = new StringBuilder(HEADER).append("\n");
    this.simplePlaylistEntries.forEach(
        (resolution, simplePlaylists) -> {
          // Add each variant
          if(simplePlaylists.size() > 1) {
            simplePlaylists.forEach(
                simplePlaylistEntry -> stringBuilder.append(simplePlaylistEntry).append("\n"));
          }
          // Add default playlist for each resolution
          stringBuilder.append(getPlaylistIdentifier(simplePlaylists.get(0)));
        });

    return stringBuilder.toString();
  }
  
  @NotNull
  private String getPlaylistIdentifier(@NotNull SimplePlaylistEntry playlistEntry) {
    return STREAM_INF
        + BANDWIDTH_TAG
        + playlistEntry.bitrate
        + ","
        + RESOLUTION_TAG
        + playlistEntry.resolution.getWidth() + "x" + playlistEntry.resolution.getHeight()
        + ","
        + VIDEO_TAG
        + "\""
        + playlistEntry.resolution.getName()
        + "\"\n"
        + playlistEntry.getSimplePlaylistUrl()
        + "\n";
  }
}
