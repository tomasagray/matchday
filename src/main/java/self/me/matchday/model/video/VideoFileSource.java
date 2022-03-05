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

package self.me.matchday.model.video;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a collection of files which compose an Event. Includes metadata describing the media
 * stream and its origin.
 */
@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VideoFileSource implements Comparable<VideoFileSource> {

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  private UUID fileSrcId;

  @OneToMany(
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      fetch = FetchType.EAGER)
  @Builder.Default
  private List<VideoFilePack> videoFilePacks = new ArrayList<>();

  private String channel;
  private String source;
  private String approximateDuration;
  private String languages;
  private Resolution resolution;
  private String mediaContainer;
  private String videoCodec;
  private String audioCodec;
  private Long videoBitrate;
  private Long audioBitrate;
  private Long filesize;
  private int framerate;
  private String audioChannels;

  public boolean addVideoFilePack(@NotNull VideoFilePack filePack) {
    return this.videoFilePacks.add(filePack);
  }

  public boolean removeVideoFilePack(VideoFilePack filePack) {
    return this.videoFilePacks.remove(filePack);
  }

  /**
   * Defensively copies VideoFilePack list
   *
   * @return Unmodifiable list
   */
  public List<VideoFilePack> getVideoFilePacks() {
    return videoFilePacks != null ? Collections.unmodifiableList(videoFilePacks) : null;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof VideoFileSource)) {
      return false;
    }
    // Cast for comparison
    final VideoFileSource videoFileSource = (VideoFileSource) o;
    return this.getChannel() != null
        && this.getChannel().equals(videoFileSource.getChannel())
        && this.getLanguages() != null
        && this.getLanguages().equals(videoFileSource.getLanguages())
        && this.getResolution() != null
        && this.getResolution().equals(videoFileSource.getResolution());
  }

  @Override
  public int compareTo(@NotNull VideoFileSource entity) {
    if (getResolution() == null || entity.getResolution() == null) {
      return -1;
    }
    if (entity.getResolution().equals(getResolution())) {
      return getAudioChannels().compareTo(entity.getAudioChannels());
    }
    // Default behavior: compare by resolution
    return getResolution().compareTo(entity.getResolution());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        fileSrcId,
        videoFilePacks,
        channel,
        source,
        approximateDuration,
        languages,
        resolution,
        mediaContainer,
        videoCodec,
        audioCodec,
        videoBitrate,
        audioBitrate,
        filesize,
        framerate,
        audioChannels);
  }
}
