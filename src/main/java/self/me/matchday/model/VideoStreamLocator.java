/*
 * Copyright (c) 2020.
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
import self.me.matchday.db.converter.PathConverter;

import javax.persistence.*;
import java.nio.file.Path;
import java.time.Instant;

@Entity
@Data
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class VideoStreamLocator {

  @Id @GeneratedValue protected Long streamLocatorId;

  @Convert(converter = PathConverter.class)
  protected Path playlistPath;

  @ManyToOne(cascade = {CascadeType.MERGE})
  protected EventFile eventFile;

  @EqualsAndHashCode.Exclude
  protected final Instant timestamp = Instant.now();

}
