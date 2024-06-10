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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.*;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.controller.converter.VideoFilesDeserializer;

@ToString
@Entity
public class VideoFilePack {

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
  @MapKeyEnumerated
  @MapKeyColumn(name = "pack_id")
  @JsonDeserialize(using = VideoFilesDeserializer.class)
  private Map<PartIdentifier, VideoFile> videoFiles = new ConcurrentSkipListMap<>();

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Type(type = "uuid-char")
  private UUID id;

  public void put(@NotNull VideoFile videoFile) {
    videoFiles.putIfAbsent(videoFile.getTitle(), videoFile);
  }

  public void putAll(@NotNull Map<PartIdentifier, VideoFile> files) {
    for (VideoFile videoFile : files.values()) {
      put(videoFile);
    }
  }

  public VideoFile get(@NotNull PartIdentifier title) {
    return videoFiles.get(title);
  }

  public Map<PartIdentifier, VideoFile> allFiles() {
    return videoFiles;
  }

  /**
   * Gets the first VideoFile in the pack, by PartIdentifier
   *
   * @return The first VideoFile in the pack, or null if pack is empty
   */
  public VideoFile firstPart() {
    if (!videoFiles.isEmpty()) {
      final List<VideoFile> sorted = getSortedVideoFiles();
      return sorted.get(0);
    }
    return null;
  }

  @NotNull
  private List<VideoFile> getSortedVideoFiles() {
    return videoFiles.entrySet().stream()
        .sorted(Entry.comparingByKey(Enum::compareTo))
        .map(Entry::getValue)
        .collect(Collectors.toList());
  }

  /**
   * Gets the last VideoFile in the pack, by PartIdentifier
   *
   * @return The last VideoFile, or null if the pack is empty
   */
  public VideoFile lastPart() {
    if (!videoFiles.isEmpty()) {
      final List<VideoFile> sorted = getSortedVideoFiles();
      final int lastIndex = sorted.size() - 1;
      return sorted.get(lastIndex);
    }
    return null;
  }

  public int size() {
    return videoFiles.size();
  }

  public void forEachVideoFile(BiConsumer<PartIdentifier, VideoFile> fn) {
    videoFiles.forEach(fn);
  }

  public boolean containsAny(@NotNull Collection<VideoFile> videoFiles) {
    return videoFiles.stream().anyMatch(this.videoFiles::containsValue);
  }

  public Stream<VideoFile> stream() {
    return videoFiles.values().stream();
  }
}
