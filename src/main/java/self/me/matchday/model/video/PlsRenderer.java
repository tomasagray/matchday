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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class PlsRenderer implements VideoPlaylistRenderer {

  private final List<MediaSegment> mediaSegments = new ArrayList<>();

  @Override
  public void addMediaSegment(@NotNull URI uri, @Nullable String title, @Nullable Double duration) {
    this.mediaSegments.add(new MediaSegment(uri, title, duration));
  }

  @Override
  public String renderPlaylist() {
    final StringBuilder sb = new StringBuilder("[playlist]\n\n");
    final int segmentCount = mediaSegments.size();
    for (int i = 0; i < segmentCount; ++i) {
      final MediaSegment mediaSegment = mediaSegments.get(i);
      final URI uri = mediaSegment.getUri();
      final String title = mediaSegment.getTitle();
      final int fileNum = i + 1; // PLS must start with File1...
      sb.append("File").append(fileNum).append("=").append(uri).append("\n");
      sb.append("Title").append(fileNum).append("=").append(title).append("\n");
    }
    sb.append("\n").append("NumberOfEntries=").append(segmentCount).append("\n");
    return sb.toString();
  }
}
