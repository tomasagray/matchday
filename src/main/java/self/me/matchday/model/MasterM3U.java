/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.EventFileSource.Resolution;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public final class MasterM3U extends M3UPlaylist {

  private static final String LOG_TAG = "MasterM3U";

  private final List<VariantPlaylistEntry> variantPlaylistEntries = new ArrayList<>();

  public void addVariant(@NotNull final Resolution resolution,
      @NotNull final List<String> languages, final long bitrate,
      @NotNull final URI playlistLink) {

    // Create variant
    final VariantPlaylistEntry playlistEntry =
        new VariantPlaylistEntry(resolution, languages, bitrate, playlistLink);
    if (variantPlaylistEntries.size() == 0) {
      playlistEntry.setDefault(true);
    }
    variantPlaylistEntries.add(playlistEntry);
  }

  /**
   * Output a standards-conforming, UTF-8 encoded playlist (.m3u8 file). See:
   * https://tools.ietf.org/html/rfc8216
   *
   * @return The formatted playlist
   */
  @NotNull
  @Override
  public String toString() {

    // Result container
    final StringBuilder stringBuilder = new StringBuilder(HEADER).append("\n");

    // Group file sources by video resolution & sort within resolution
    final TreeMap<Resolution, List<VariantPlaylistEntry>> variantPlaylistEntries =
        getVariantPlaylistEntries()
            .stream()
            .collect(groupingBy(VariantPlaylistEntry::getResolution, TreeMap::new, toList()));

    // Add each variant to output String
    AtomicBoolean first = new AtomicBoolean(true);
    variantPlaylistEntries.forEach(
        (resolution, variantPlaylists) -> {
          if (variantPlaylists.size() > 1) {
            variantPlaylists.forEach(
                variantPlaylistEntry -> {
                  // Set first variant to default
                  if (first.get()) {
                    variantPlaylistEntry.setDefault(true);
                    first.set(false);
                  }
                  // Add each variant to output String
                  stringBuilder.append(variantPlaylistEntry).append("\n");
                });
          }
          // Specify default playlist for each resolution
          stringBuilder.append(variantPlaylists.get(0).getPlaylistIdentifier());
        });

    return stringBuilder.toString();
  }

  @Data
  @NoArgsConstructor
  public static class VariantPlaylistEntry {

    private static final int BITRATE_INDEX = 0;
    // M3UPlaylist extended tags
    private static final String STREAM_INF = "#EXT-X-STREAM-INF:";
    private static final String BANDWIDTH_TAG = "BANDWIDTH=";
    private static final String VIDEO_TAG = "VIDEO=";
    private static final String RESOLUTION_TAG = "RESOLUTION=";
    private static final String MEDIA_LINK = "#EXT-X-MEDIA:";  // for linking multiple versions
    private static final String MEDIA_TYPE = "TYPE=VIDEO";
    private static final String GROUP_ID_TAG = "GROUP-ID=";
    private static final String NAME_TAG = "NAME=";
    private static final String DEFAULT_TAG = "DEFAULT=";
    private static final String URI_TAG = "URI=";

    private URI playlistLink;
    private Resolution resolution;
    private List<String> languages;
    private long bitrate;
    private boolean isDefault;

    VariantPlaylistEntry(@NotNull final Resolution resolution,
        @NotNull final List<String> languages, final long bitrate,
        @NotNull final URI playlistLink) {

      this.resolution = resolution;
      this.languages = languages;
      this.bitrate = bitrate;
      this.playlistLink = playlistLink;
    }

    @Override
    public String toString() {
      return MEDIA_LINK
          + MEDIA_TYPE
          + ","
          + GROUP_ID_TAG
          + "\""
          + getResolution()
          + "\","
          + NAME_TAG
          + "\""
          + String.join("/", getLanguages())
          + "\","
          + "DEFAULT=" + (isDefault ? "YES" : "NO") + ","
          + URI_TAG
          + "\""
          + getPlaylistLink()
          + "\"";
    }

    @NotNull
    String getPlaylistIdentifier() {
      return STREAM_INF
          + BANDWIDTH_TAG
          + getBitrate()
          + ","
          + RESOLUTION_TAG
          + getResolution().getWidth() + "x" + getResolution().getHeight()
          + ","
          + VIDEO_TAG
          + "\""
          + getResolution()
          + "\"\n"
          + getPlaylistLink()
          + "\n";
    }
  }
}
