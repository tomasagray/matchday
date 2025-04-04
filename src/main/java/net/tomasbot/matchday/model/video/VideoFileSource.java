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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.*;
import javax.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.api.controller.converter.ResolutionDeserializer;

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
  @Type(type = "uuid-char")
  private UUID fileSrcId;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
  @Builder.Default
  private List<VideoFilePack> videoFilePacks = new ArrayList<>();

  private String channel;
  private String source;
  private String approximateDuration;
  private String languages;

  @JsonDeserialize(using = ResolutionDeserializer.class)
  private Resolution resolution;

  private String mediaContainer;
  private String videoCodec;
  private String audioCodec;
  private Long videoBitrate;
  private Long audioBitrate;
  private Long filesize;
  private int framerate;
  private String audioChannels;

  public void addAllVideoFilePacks(@NotNull Collection<VideoFilePack> filePacks) {
    for (final VideoFilePack pack : filePacks) {
      addVideoFilePack(pack);
    }
  }

  public void addVideoFilePack(@NotNull VideoFilePack filePack) {
    VideoFilePack existing = null;
    for (final VideoFilePack pack : this.videoFilePacks) {
      if (pack.containsAny(filePack.allFiles().values())) {
        // avoid ConcurrentModificationException
        existing = pack;
        break;
      }
    }
    if (existing != null) {
      existing.putAll(filePack.allFiles());
    } else {
      this.videoFilePacks.add(filePack);
    }
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof VideoFileSource that)) return false;
    return getFramerate() == that.getFramerate()
        && Objects.equals(getChannel(), that.getChannel())
        && Objects.equals(getSource(), that.getSource())
        && Objects.equals(getApproximateDuration(), that.getApproximateDuration())
        && Objects.equals(getLanguages(), that.getLanguages())
        && getResolution() == that.getResolution()
        && Objects.equals(getMediaContainer(), that.getMediaContainer())
        && Objects.equals(getVideoCodec(), that.getVideoCodec())
        && Objects.equals(getAudioCodec(), that.getAudioCodec())
        && Objects.equals(getVideoBitrate(), that.getVideoBitrate())
        && Objects.equals(getAudioBitrate(), that.getAudioBitrate())
        && Objects.equals(getFilesize(), that.getFilesize())
        && Objects.equals(getAudioChannels(), that.getAudioChannels());
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
