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

import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@ToString
@Entity
public class VideoFilePack {

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @MapKeyEnumerated
  @MapKeyColumn(name = "pack_id")
  private final Map<PartIdentifier, VideoFile> videoFiles;

  public VideoFilePack() {
    this.videoFiles = new ConcurrentSkipListMap<>();
  }

  public boolean put(@NotNull VideoFile videoFile) {
    final VideoFile added = videoFiles.putIfAbsent(videoFile.getTitle(), videoFile);
    return added == null;
  }

  public VideoFile get(@NotNull PartIdentifier title) {
    return videoFiles.get(title);
  }

  public VideoFile firstPart() {
    final Map.Entry<PartIdentifier, VideoFile> firstEntry =
        ((NavigableMap<PartIdentifier, VideoFile>) videoFiles).firstEntry();
    return firstEntry != null ? firstEntry.getValue() : null;
  }

  public VideoFile lastPart() {
    final Map.Entry<PartIdentifier, VideoFile> lastEntry =
        ((NavigableMap<PartIdentifier, VideoFile>) videoFiles).lastEntry();
    return lastEntry != null ? lastEntry.getValue() : null;
  }

  public int size() {
    return videoFiles.size();
  }

  public void forEach(BiConsumer<PartIdentifier, VideoFile> fn) {
    videoFiles.forEach(fn);
  }

  public Stream<VideoFile> stream() {
    return videoFiles.values().stream();
  }
}
