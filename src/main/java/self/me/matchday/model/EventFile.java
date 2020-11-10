/*
 * Copyright (c) 2020.
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

package self.me.matchday.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.db.converter.FFmpegMetadataConverter;
import self.me.matchday.plugin.io.ffmpeg.FFmpegMetadata;

import javax.persistence.*;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.regex.Pattern;

@Entity
@Data
@NoArgsConstructor
public class EventFile implements Comparable<EventFile> {

  private static double DEFAULT_DURATION = 3012.541956d;

  // Fields
  @Id
  @GeneratedValue
  private Long eventFileId;
  private EventPartIdentifier title;
  private URL externalUrl;
  // refreshed data
  @Column(columnDefinition = "LONGTEXT")
  private URL internalUrl;
  @Convert(converter = FFmpegMetadataConverter.class)
  @Column(columnDefinition = "LONGTEXT")
  private FFmpegMetadata metadata;
  private Timestamp lastRefreshed = new Timestamp(0L);

  public EventFile(@NotNull final EventPartIdentifier title, @NotNull final URL externalUrl) {

    this.title = title;
    this.externalUrl = externalUrl;
    this.internalUrl = null;
    this.metadata = null;
  }

  /**
   * Returns the duration of this EventFile, in milliseconds.
   *
   * @return The duration of this EventFile (millis).
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
    if (!(obj instanceof EventFile)) {
      return false;
    }
    // Cast
    final EventFile eventFile = (EventFile) obj;
    return this.getEventFileId().equals(eventFile.getEventFileId());
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(getEventFileId(), getTitle(), getExternalUrl(), getInternalUrl(), getMetadata(),
            getLastRefreshed());
  }

  @Override
  public int compareTo(@NotNull EventFile test) {
    return this.getTitle().order - test.getTitle().order;
  }

  /**
   * Event part identifiers
   */
  public enum EventPartIdentifier {

    DEFAULT("", "", -1),
    PRE_MATCH("Pre-Match", "^[Pp][Rr][Ee][- ][Mm][Aa][Tt][Cc][Hh]$", 0),
    FIRST_HALF("1st Half", "1 ?[Ss][Tt] [Hh][Aa][Ll][Ff]", 1),
    SECOND_HALF("2nd Half", "2 ?[Nn][Dd] [Hh][Aa][Ll][Ff]", 2),
    EXTRA_TIME("Extra-Time/Penalties", "^[Ee][Xx][Tt][Rr][Aa][- ][Tt][Ii][Mm][Ee]", 3),
    TROPHY_CEREMONY("Trophy Ceremony", "^[Tt][Rr][Oo][Pp][Hh][Yy]", 4),
    POST_MATCH("Post-Match", "^[Pp][Oo][Ss][Tt][- ][Mm][Aa][Tt][Cc][Hh]$", 5);

    private final String name;
    private final Pattern pattern;
    private final int order;

    EventPartIdentifier(@NotNull String name, @NotNull String pattern, int order) {
      this.name = name;
      this.pattern = Pattern.compile(pattern);
      this.order = order;
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
     * values.
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
