/*
 * Copyright (c) 2022.
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

package net.tomasbot.matchday.model.video;

import java.net.URL;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.tomasbot.ffmpeg_wrapper.metadata.FFmpegMetadata;
import net.tomasbot.matchday.db.converter.FFmpegMetadataConverter;
import net.tomasbot.matchday.db.converter.TimestampConverter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class VideoFile implements Comparable<VideoFile> {
  
  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Type(type = "uuid-char")
  private UUID fileId;

  private URL externalUrl;
  private PartIdentifier title;

  @Column(columnDefinition = "LONGTEXT")
  private URL internalUrl;

  @Convert(converter = FFmpegMetadataConverter.class)
  @Column(columnDefinition = "LONGTEXT")
  private FFmpegMetadata metadata;

  @Convert(converter = TimestampConverter.class)
  private Timestamp lastRefreshed = new Timestamp(0L);

  public VideoFile(@NotNull final PartIdentifier title, @NotNull final URL externalUrl) {
    this.title = title;
    this.externalUrl = externalUrl;
  }

  /**
   * Returns the duration of this VideoFile, in milliseconds, or -1 if the actual duration cannot be
   * determined.
   *
   * @return The duration of this VideoFile (millis).
   */
  public double getDuration() {
    if (getMetadata() != null && getMetadata().getFormat() != null) {
      return getMetadata().getFormat().getDuration();
    } else {
      return -1;
    }
  }

  public String toString() {
    return String.format("%s - %s", getTitle(), getExternalUrl());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof final VideoFile videoFile)) return false;
    return Objects.equals(videoFile.getExternalUrl(), this.getExternalUrl());
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileId, externalUrl, title, internalUrl, metadata, lastRefreshed);
  }

  @Override
  public int compareTo(@NotNull VideoFile test) {
    final PartIdentifier otherTitle = test.getTitle();
    return this.getTitle().compareTo(otherTitle);
  }
}
