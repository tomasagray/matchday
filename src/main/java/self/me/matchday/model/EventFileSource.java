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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a collection of files which compose an Event. Includes metadata describing the media
 * stream and its origin.
 */
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventFileSource implements Comparable<EventFileSource> {

  /** Video resolution classes */
  public enum Resolution {
    R_4k("4K", 3840, 2160),
    R_1080p("1080p", 1920, 1080),
    R_1080i("1080i", 1920, 1080),
    R_720p("720p", 1280, 720),
    R_576p("576p", 768, 576),
    R_SD("SD", 640, 480);

    // Fields
    private final String name;
    private final Pattern pattern;
    private final int width;
    private final int height;

    @Contract(pure = true)
    Resolution(@NotNull String name, int width, int height) {
      this.name = name;
      this.pattern = Pattern.compile(".*" + name + ".*");
      this.width = width;
      this.height = height;
    }

    @Override
    public String toString() {
      return this.name;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }

    /**
     * Determines if the supplied String corresponds to an enumerated video resolution.
     *
     * @param str The test String
     * @return True / false.
     */
    public static boolean isResolution(@NotNull String str) {
      return Arrays.stream(Resolution.values())
          .map(resolution -> resolution.pattern.matcher(str))
          .anyMatch(Matcher::matches);
    }

    /**
     * Factory method to return an enumerated video resolution from a given String.
     *
     * @param str The String to be converted.
     * @return The Resolution value, or <b>null</b> if the given String does not match an enumerated
     *     value.
     */
    @Nullable
    @Contract(pure = true)
    public static Resolution fromString(@NotNull String str) {
      return Arrays.stream(Resolution.values())
          .filter(resolution -> resolution.pattern.matcher(str).matches())
          .findFirst()
          .orElse(null);
    }
  }

  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name = "system-uuid", strategy = "org.hibernate.id.UUIDGenerator")
  private String eventFileSrcId;

  private String channel;
  private String source;
  private String approximateDuration;
  private Long fileSize;
  private String languages;

  @OneToMany(targetEntity = EventFile.class, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private final List<EventFile> eventFiles = new ArrayList<>();
  // Media metadata
  private Resolution resolution;
  private String mediaContainer;
  private Long bitrate;
  private String videoCodec;
  private int frameRate;
  private String audioCodec;

  private int audioChannels;

  public String toString() {

    return String.format(
        "%s (%s) - %s, %s files",
        getChannel(), getResolution(), getLanguages(), getEventFiles().size());
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof EventFileSource)) {
      return false;
    }
    // Cast for comparison
    final EventFileSource eventFileSource = (EventFileSource) o;
    return this.getChannel() != null
        && this.getChannel().equals(eventFileSource.getChannel())
        && this.getLanguages() != null
        && this.getLanguages().equals(eventFileSource.getLanguages())
        && this.getResolution() != null
        && this.getResolution().equals(eventFileSource.getResolution());
  }

  @Override
  public int compareTo(@NotNull EventFileSource entity) {

    // Null resolutions are less-than by definition
    if (getResolution() == null || entity.getResolution() == null) {
      return -1;
    }
    // If the resolutions are the same...
    if (entity.getResolution().equals(getResolution())) {
      // ... use audio channels
      return getAudioChannels() - entity.getAudioChannels();
    }
    // Default behavior: compare by resolution
    return getResolution().compareTo(entity.getResolution());
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + ((channel != null) ? channel.hashCode() : 0);
    hash = 31 * hash + ((languages != null) ? languages.hashCode() : 0);
    hash = 31 * hash + ((resolution != null) ? resolution.hashCode() : 0);
    return hash;
  }
}
