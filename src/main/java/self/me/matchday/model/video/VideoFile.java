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
    return this.getTitle().compareTo(test.getTitle());
  }

  /** Event part identifiers */
  public enum EventPartIdentifier {
    DEFAULT(""),
    PRE_MATCH("Pre-Match"),
    FIRST_HALF("1st Half"),
    SECOND_HALF("2nd Half"),
    EXTRA_TIME("Extra-Time/Penalties"),
    TROPHY_CEREMONY("Trophy Ceremony"),
    POST_MATCH("Post-Match");

    private final String name;

    EventPartIdentifier(@NotNull String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }
}
