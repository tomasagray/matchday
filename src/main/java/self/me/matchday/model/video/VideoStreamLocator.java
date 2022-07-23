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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.db.converter.PathConverter;

import javax.persistence.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class VideoStreamLocator {

  @Id @GeneratedValue protected Long streamLocatorId;

  @Convert(converter = PathConverter.class)
  protected Path playlistPath;

  @EqualsAndHashCode.Exclude protected final Instant timestamp = Instant.now();

  @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
  protected VideoFile videoFile;

  @OneToOne(cascade = CascadeType.ALL)
  protected TaskState state = TaskState.builder().build();

  public void updateState(
      @NotNull final StreamJobState.JobStatus status, final Double completionRatio) {
    this.state.setStatus(status);
    this.state.setCompletionRatio(completionRatio);
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
    if (!(o instanceof VideoStreamLocator)) return false;
    final VideoStreamLocator that = (VideoStreamLocator) o;
    return Objects.equals(getPlaylistPath(), that.getPlaylistPath())
        && Objects.equals(getTimestamp(), that.getTimestamp())
        && Objects.equals(getVideoFile(), that.getVideoFile())
        && Objects.equals(getState(), that.getState());
  }
}
