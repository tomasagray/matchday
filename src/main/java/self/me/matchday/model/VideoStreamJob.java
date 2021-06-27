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

package self.me.matchday.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class VideoStreamJob extends Thread {

  public enum JobStatus {
    CREATED,
    STARTED,
    ERROR,
    BUFFERING,
    STREAMING,
    COMPLETED,
  }

  @Id
  @GeneratedValue
  private Long jobId;
  @OneToOne(targetEntity = VideoStreamPlaylist.class)
  private final VideoStreamPlaylist streamPlaylist;
  private JobStatus status;
  @Transient
  private Collection<VideoStreamTask> streamTasks;

  public VideoStreamJob() {
    this.streamPlaylist = null;
  }

  public VideoStreamJob(@NotNull final VideoStreamPlaylist streamPlaylist) {
    this.streamPlaylist = streamPlaylist;
    this.status = JobStatus.CREATED;
    this.streamTasks = new ArrayList<>();
  }

  public void addStreamTask(@NotNull final VideoStreamTask streamTask) {
    this.streamTasks.add(streamTask);
  }

  public void removeStreamTask(@NotNull final VideoStreamTask streamTask) {
    this.streamTasks.remove(streamTask);
  }

  public void clearStreamTasks() {
    this.streamTasks.clear();
  }

  @Override
  public void interrupt() {
    super.interrupt();
    this.streamTasks.forEach(Thread::interrupt);
  }

  @Override
  public void run() {
    setStatus(JobStatus.STARTED);
    streamTasks.forEach(VideoStreamTask::run);
  }
}
