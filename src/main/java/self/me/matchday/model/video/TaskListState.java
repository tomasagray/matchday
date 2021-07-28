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

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class TaskListState extends StreamJobState {

  public static TaskListStateBuilder builder() {
    return new TaskListStateBuilder();
  }

  public static class TaskListStateBuilder {

    private Long id;
    private JobStatus status = StreamJobState.JobStatus.CREATED;
    private Double completionRatio = 0.0;

    public TaskListStateBuilder id(Long id) {
      this.id = id;
      return this;
    }

    public TaskListStateBuilder status(JobStatus status) {
      this.status = status;
      return this;
    }

    public TaskListStateBuilder completionRatio(Double completionRatio) {
      this.completionRatio = completionRatio;
      return this;
    }

    public TaskListState build() {
      final TaskListState taskListState = new TaskListState();
      taskListState.setId(this.id);
      taskListState.setStatus(this.status);
      taskListState.setCompletionRatio(this.completionRatio);
      return taskListState;
    }
  }
}
