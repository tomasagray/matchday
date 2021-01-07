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
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class VideoStreamPlaylist {

  @Id @GeneratedValue private Long id;
  @ManyToOne private final EventFileSource fileSource;

  @OneToMany(cascade = CascadeType.MERGE)
  @LazyCollection(LazyCollectionOption.FALSE)
  private final List<VideoStreamLocator> streamLocators = new ArrayList<>();

  private final Instant timestamp = Instant.now();

  public VideoStreamPlaylist() {
    this.fileSource = null;
  }

  public VideoStreamPlaylist(@NotNull final EventFileSource fileSource) {
    this.fileSource = fileSource;
  }

  public void addStreamLocator(@NotNull final VideoStreamLocator streamLocator) {
    this.streamLocators.add(streamLocator);
  }
}
