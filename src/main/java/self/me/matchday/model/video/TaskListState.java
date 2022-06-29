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

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@Builder
@Entity
public class TaskListState extends StreamJobState {

  @OneToMany(cascade = CascadeType.ALL)
  private final List<TaskState> taskStates = new ArrayList<>();

  public void addTaskState(@NotNull TaskState state) {
    taskStates.add(state);
  }

  public void computeState() {

    Double aggregateCompletionTotal = 0.0;
    for (final TaskState state : taskStates) {
      final JobStatus status = state.getStatus();
      if (status.equals(JobStatus.STOPPED) || status.equals(JobStatus.ERROR)) {
        setStatus(status);
        return;
      } else if (status.compareTo(this.getStatus()) > 0) {
        setStatus(status);
      }
      aggregateCompletionTotal += state.getCompletionRatio();
    }
    final double playlistCompletionRatio = aggregateCompletionTotal / taskStates.size();
    setCompletionRatio(playlistCompletionRatio);
  }
}
