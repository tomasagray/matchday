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
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import java.nio.file.Path;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
public class SingleStreamLocator extends VideoStreamLocator {

  public SingleStreamLocator() {
    this.playlistPath = null;
    this.videoFile = null;
  }

  public SingleStreamLocator(@NotNull final Path playlistPath, @NotNull final VideoFile videoFile) {
    this.playlistPath = playlistPath;
    this.videoFile = videoFile;
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
