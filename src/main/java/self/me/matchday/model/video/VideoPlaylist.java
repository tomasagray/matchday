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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class VideoPlaylist {

  private final Map<Long, PartIdentifier> locatorIds = new HashMap<>();
  private UUID eventId;
  private UUID fileSrcId;
  private String title;

  public VideoPlaylist(@NotNull UUID eventId, @NotNull UUID fileSrcId) {
    this.eventId = eventId;
    this.fileSrcId = fileSrcId;
  }

  public void addLocator(@NotNull Long locatorId, @NotNull PartIdentifier partId) {
    locatorIds.put(locatorId, partId);
  }
}
