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

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import javax.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.db.converter.PathConverter;
import net.tomasbot.matchday.model.video.StreamJobState.JobStatus;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class VideoStreamLocator {

  @EqualsAndHashCode.Exclude protected final Instant timestamp = Instant.now();
  @Id @GeneratedValue protected Long streamLocatorId;

  @Convert(converter = PathConverter.class)
  protected Path playlistPath;

  @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
  protected VideoFile videoFile;

  @OneToOne(cascade = CascadeType.ALL)
  protected TaskState state = new TaskState();

  public void updateState(@NotNull JobStatus status, Double completionRatio) {
    this.state.setStatus(status);
    this.state.setCompletionRatio(completionRatio);
  }

  public void updateState(
      @NotNull JobStatus status, Double completionRatio, VideoStreamingError error) {
    updateState(status, completionRatio);
    this.state.setError(error);
  }

  @Override
  public String toString() {
    return String.format(
        "<<VideoStreamLocator>>(streamLocatorId=[%s], playlistPath=[%s], timestamp=[%s], VideoFile=[%s], state=[%s])",
        streamLocatorId, playlistPath, timestamp, videoFile, state);
  }

  @Override
  public int hashCode() {
    return Objects.hash(playlistPath, timestamp, videoFile, state);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof final VideoStreamLocator that)) return false;
    return Objects.equals(getPlaylistPath(), that.getPlaylistPath())
        && Objects.equals(getTimestamp(), that.getTimestamp())
        && Objects.equals(getVideoFile(), that.getVideoFile())
        && Objects.equals(getState(), that.getState());
  }
}
