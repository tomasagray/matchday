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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public interface VideoPlaylistRenderer {

  void addMediaSegment(@NotNull URI uri, @Nullable String title, @Nullable Double duration);

  String renderPlaylist();

  /**
   * Represents a single segment (record) in the playlist, which includes the URI of the media
   * resource, its duration in seconds and an optional title.
   */
  @Data
  class MediaSegment {

    private final String title;
    private final URI uri;
    private final Double duration;

    @Contract(pure = true)
    MediaSegment(
        @NotNull final URI uri, @Nullable final String title, @Nullable final Double duration) {

      this.title = title;
      this.uri = uri;
      this.duration = duration;
    }
  }
}
