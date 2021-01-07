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
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import java.nio.file.Path;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
public class SingleStreamLocator extends VideoStreamLocator {

  public SingleStreamLocator() {
    this.playlistPath = null;
    this.eventFile = null;
  }

  public SingleStreamLocator(@NotNull final Path playlistPath, @NotNull final EventFile eventFile) {
    this.playlistPath = playlistPath;
    this.eventFile = eventFile;
  }

  public String toString() {
    return String.format(
        "SingleStreamLocator([id]=%s, [eventFile]=%s, [playlistPath]=%s, [timestamp]=%s)",
        this.streamLocatorId, this.eventFile, this.playlistPath, this.timestamp);
  }
}
