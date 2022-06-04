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
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.db.converter.PathConverter;

import javax.persistence.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
public class VideoStreamLocatorPlaylist {

  @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
  private final VideoFileSource fileSource;

  @OneToMany(cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  private final List<VideoStreamLocator> streamLocators = new ArrayList<>();

  @Convert(converter = PathConverter.class)
  private final Path storageLocation;

  @EqualsAndHashCode.Exclude private final Instant timestamp = Instant.now();
  @Id @GeneratedValue private Long id;

  @OneToOne(cascade = CascadeType.ALL)
  private TaskListState state = TaskListState.builder().build();

  public VideoStreamLocatorPlaylist() {
    this.fileSource = null;
    this.storageLocation = null;
  }

  public VideoStreamLocatorPlaylist(
      @NotNull final VideoFileSource fileSource, @NotNull final Path storageLocation) {
    this.fileSource = fileSource;
    this.storageLocation = storageLocation;
  }

  public void addStreamLocator(@NotNull final VideoStreamLocator streamLocator) {
    this.streamLocators.add(streamLocator);
  }

  public TaskListState getState() {
    // ensure state is fresh
    computePlaylistState();
    return this.state;
  }

  /** Compute the aggregate state of the playlist from the states of each of its locators */
  private void computePlaylistState() {

    // compute aggregate state
    Double aggregateCompletionTotal = 0.0;
    final TaskListState listState = this.state;
    final List<VideoStreamLocator> streamLocators = this.getStreamLocators();

    for (VideoStreamLocator streamLocator : streamLocators) {
      final TaskState taskState = streamLocator.getState();
      final StreamJobState.JobStatus jobStatus = taskState.getStatus();
      // increase job status to highest of subtasks
      if (jobStatus.compareTo(listState.getStatus()) > 0) {
        listState.setStatus(jobStatus);
      }
      aggregateCompletionTotal += taskState.getCompletionRatio();
    }
    // compute aggregate completion total
    final double playlistCompletionRatio = aggregateCompletionTotal / streamLocators.size();
    listState.setCompletionRatio(playlistCompletionRatio);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    VideoStreamLocatorPlaylist that = (VideoStreamLocatorPlaylist) o;

    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return 1304080917;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "("
        + "id = "
        + id
        + ", "
        + "fileSource = "
        + fileSource
        + ", "
        + "storageLocation = "
        + storageLocation
        + ", "
        + "timestamp = "
        + timestamp
        + ", "
        + "state = "
        + state
        + ")";
  }
}
