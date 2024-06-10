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

import java.util.Objects;
import javax.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import self.me.matchday.db.converter.VideoStreamingErrorConverter;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class StreamJobState {

  protected JobStatus status = JobStatus.CREATED;
  protected Double completionRatio = 0.0;

  @Column(columnDefinition = "LONGTEXT")
  @Convert(converter = VideoStreamingErrorConverter.class)
  protected VideoStreamingError error;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StreamJobState that)) return false;
    return Objects.equals(getId(), that.getId())
        && getStatus() == that.getStatus()
        && Objects.equals(getCompletionRatio(), that.getCompletionRatio());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getStatus(), getCompletionRatio());
  }

  public enum JobStatus {
    ERROR,
    STOPPED,
    CREATED,
    QUEUED,
    STARTED,
    BUFFERING,
    STREAMING,
    COMPLETED,
  }
}
