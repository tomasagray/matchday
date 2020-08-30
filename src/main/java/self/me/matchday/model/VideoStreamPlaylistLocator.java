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

import java.io.Serializable;
import java.nio.file.Path;
import java.util.UUID;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import self.me.matchday.db.converter.PathConverter;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoStreamPlaylistLocator {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Embeddable
  public static class VideoStreamPlaylistId implements Serializable {
    private String eventId;
    private UUID fileSrcId;
  }

  @EmbeddedId
  private VideoStreamPlaylistId playlistId;
  @Convert(converter = PathConverter.class)
  private Path playlistPath;

}
