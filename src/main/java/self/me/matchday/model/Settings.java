/*
 * Copyright (c) 2023.
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

import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.scheduling.support.CronTrigger;
import self.me.matchday.db.converter.CronTriggerConverter;
import self.me.matchday.db.converter.PathConverter;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Settings {

  private final Timestamp timestamp;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Convert(converter = PathConverter.class)
  private Path logFilename;

  @Convert(converter = PathConverter.class)
  private Path artworkStorageLocation;

  @Convert(converter = PathConverter.class)
  private Path videoStorageLocation;

  @Convert(converter = PathConverter.class)
  private Path backupLocation;

  @Convert(converter = CronTriggerConverter.class)
  private CronTrigger refreshEvents;

  @Convert(converter = CronTriggerConverter.class)
  private CronTrigger pruneVideos;

  private int videoExpiredDays;

  public Settings() {
    this.timestamp = Timestamp.from(Instant.now());
  }
}
