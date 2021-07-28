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
public class TaskState extends StreamJobState {

  public static TaskStateBuilder builder() {
    return new TaskStateBuilder();
  }

  public static class TaskStateBuilder {

    private Long id;
    private JobStatus status = StreamJobState.JobStatus.CREATED;
    private Double completionRatio = 0.0;

    public TaskStateBuilder id(Long id) {
      this.id = id;
      return this;
    }

    public TaskStateBuilder status(JobStatus status) {
      this.status = status;
      return this;
    }

    public TaskStateBuilder completionRatio(Double completionRatio) {
      this.completionRatio = completionRatio;
      return this;
    }

    public TaskState build() {
      final TaskState taskState = new TaskState();
      taskState.setId(this.id);
      taskState.setStatus(this.status);
      taskState.setCompletionRatio(this.completionRatio);
      return taskState;
    }
  }
}
