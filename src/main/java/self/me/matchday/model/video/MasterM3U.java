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
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.EventFileSource.Resolution;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public final class MasterM3U extends M3U {

  private static final String LOG_TAG = "MasterM3U";

  private final List<VariantPlaylistEntry> variantPlaylistEntries = new ArrayList<>();

  public MasterM3U addVariant(
      @NotNull final Resolution resolution,
      final String languages,
      final long bitrate,
      @NotNull final URI playlistLink) {

    // Create variant
    final VariantPlaylistEntry playlistEntry =
        new VariantPlaylistEntry(resolution, languages, bitrate, playlistLink);
    if (variantPlaylistEntries.size() == 0) {
      playlistEntry.setDefault(true);
    }
    // Add to collection
    variantPlaylistEntries.add(playlistEntry);
    // For fluency
    return this;
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
        getVariantPlaylistEntries().stream()
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
    // M3U extended tags
    private static final String STREAM_INF = "#EXT-X-STREAM-INF:";
    private static final String BANDWIDTH_TAG = "BANDWIDTH=";
    private static final String VIDEO_TAG = "VIDEO=";
    private static final String RESOLUTION_TAG = "RESOLUTION=";
    private static final String MEDIA_LINK = "#EXT-X-MEDIA:"; // for linking multiple versions
    private static final String MEDIA_TYPE = "TYPE=VIDEO";
    private static final String GROUP_ID_TAG = "GROUP-ID=";
    private static final String NAME_TAG = "NAME=";
    private static final String DEFAULT_TAG = "DEFAULT=";
    private static final String URI_TAG = "URI=";

    private URI playlistLink;
    private Resolution resolution;
    private String languages;
    private long bitrate;
    private boolean isDefault;

    VariantPlaylistEntry(
        @NotNull final Resolution resolution,
        final String languages,
        final long bitrate,
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
          + "DEFAULT="
          + (isDefault ? "YES" : "NO")
          + ","
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
          + getResolution().getWidth()
          + "x"
          + getResolution().getHeight()
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