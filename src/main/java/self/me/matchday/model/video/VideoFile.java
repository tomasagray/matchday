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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.db.converter.FFmpegMetadataConverter;
import self.me.matchday.db.converter.TimestampConverter;
import self.me.matchday.model.MD5String;
import self.me.matchday.plugin.io.ffmpeg.FFmpegMetadata;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.regex.Pattern;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
public class VideoFile implements Comparable<VideoFile> {

  private static double DEFAULT_DURATION = 3012.541956d;

  // Fields
  @Id private String fileId;
  private URL externalUrl;
  private EventPartIdentifier title;

  @Column(columnDefinition = "LONGTEXT")
  private URL internalUrl;

  @Convert(converter = FFmpegMetadataConverter.class)
  @Column(columnDefinition = "LONGTEXT")
  private FFmpegMetadata metadata;

  @Convert(converter = TimestampConverter.class)
  private Timestamp lastRefreshed = new Timestamp(0L);

  public VideoFile(@NotNull final EventPartIdentifier title, @NotNull final URL externalUrl) {

    this.fileId = MD5String.fromData(externalUrl);
    this.title = title;
    this.externalUrl = externalUrl;
    this.internalUrl = null;
    this.metadata = null;
  }

  public void setFileId(String id) {
    this.fileId = id;
  }

  /**
   * Returns the duration of this VideoFile, in milliseconds.
   *
   * @return The duration of this VideoFile (millis).
   */
  public double getDuration() {
    if (getMetadata() != null && getMetadata().getFormat() != null) {
      return getMetadata().getFormat().getDuration();
    } else {
      return DEFAULT_DURATION;
    }
  }

  public String toString() {
    return String.format("%s - %s", getTitle(), getExternalUrl().toString());
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof VideoFile)) {
      return false;
    }
    // Cast
    final VideoFile videoFile = (VideoFile) obj;
    return this.getExternalUrl().equals(videoFile.getExternalUrl());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getTitle(), getExternalUrl(), getInternalUrl(), getMetadata(), getLastRefreshed());
  }

  @Override
  public int compareTo(@NotNull VideoFile test) {
    return this.getTitle().order - test.getTitle().order;
  }

  /** Event part identifiers */
  public enum EventPartIdentifier {
    DEFAULT("", "", 0),
    PRE_MATCH("Pre-Match", "^[Pp][Rr][Ee][- ][Mm][Aa][Tt][Cc][Hh]$", 1),
    FIRST_HALF("1st Half", "1 ?[Ss][Tt] [Hh][Aa][Ll][Ff]", 2),
    SECOND_HALF("2nd Half", "2 ?[Nn][Dd] [Hh][Aa][Ll][Ff]", 3),
    EXTRA_TIME("Extra-Time/Penalties", "^[Ee][Xx][Tt][Rr][Aa][- ][Tt][Ii][Mm][Ee]", 4),
    TROPHY_CEREMONY("Trophy Ceremony", "^[Tt][Rr][Oo][Pp][Hh][Yy]", 5),
    POST_MATCH("Post-Match", "^[Pp][Oo][Ss][Tt][- ][Mm][Aa][Tt][Cc][Hh]$", 6);

    private final String name;
    private final Pattern pattern;
    private final int order;

    EventPartIdentifier(@NotNull String name, @NotNull String pattern, int order) {
      this.name = name;
      this.pattern = Pattern.compile(pattern);
      this.order = order;
    }

    public int getOrder() {
      return this.order;
    }

    @Override
    public String toString() {
      return this.name;
    }

    /**
     * Determines if the given String corresponds to an enumerated Event part identifier.
     *
     * @param str The test String
     * @return True / false.
     */
    public static boolean isPartIdentifier(@NotNull String str) {
      return PRE_MATCH.pattern.matcher(str).find()
          || FIRST_HALF.pattern.matcher(str).find()
          || SECOND_HALF.pattern.matcher(str).find()
          || EXTRA_TIME.pattern.matcher(str).find()
          || TROPHY_CEREMONY.pattern.matcher(str).find()
          || POST_MATCH.pattern.matcher(str).find();
    }

    /**
     * Factory method to convert a String to an enumerated Event part identifier.
     *
     * @param str The String to be converted.
     * @return The enumerated value, or <b>DEFAULT</b> if the given String does not match any
     *     values.
     */
    public static EventPartIdentifier fromString(@NotNull String str) {
      // If the given String doesn't match
      EventPartIdentifier result = DEFAULT;

      for (EventPartIdentifier partIdentifier : EventPartIdentifier.values()) {
        if (partIdentifier.pattern.matcher(str).matches()) {
          result = partIdentifier;
          break;
        }
      }

      return result;
    }
  }
}
