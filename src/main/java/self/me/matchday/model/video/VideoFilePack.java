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
import self.me.matchday.model.video.VideoFile.EventPartIdentifier;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@Entity
public class VideoFilePack {

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  /*@CollectionTable(
  name = "video_file_pack_mapping",
  joinColumns = {@JoinColumn(name = "file_pack_id", referencedColumnName = "id")})*/
  @MapKeyEnumerated
  @MapKeyColumn(name = "pack_id")
  private final Map<EventPartIdentifier, VideoFile> videoFiles;

  @Id @GeneratedValue Long id;

  public VideoFilePack() {
    this.videoFiles = new HashMap<>();
  }

  public VideoFilePack(@NotNull Map<EventPartIdentifier, VideoFile> videoFiles) {
    this.videoFiles = videoFiles;
  }

  public boolean put(@NotNull VideoFile videoFile) {
    final VideoFile added = videoFiles.putIfAbsent(videoFile.getTitle(), videoFile);
    return added == null;
  }

  public VideoFile get(@NotNull EventPartIdentifier title) {
    return videoFiles.get(title);
  }

  public int size() {
    return videoFiles.size();
  }

  public void forEach(BiConsumer<EventPartIdentifier, VideoFile> fn) {
    videoFiles.forEach(fn);
  }

  public Stream<VideoFile> stream() {
    return videoFiles.values().stream();
  }
}
