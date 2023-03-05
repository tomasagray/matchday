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

package self.me.matchday.model;

import lombok.*;
import org.hibernate.Hibernate;
import self.me.matchday.db.converter.PathConverter;

import javax.persistence.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artwork {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

  @Convert(converter = PathConverter.class)
  private Path file;

  private Long fileSize;
  private String mediaType;
  private int width;
  private int height;
  private LocalDateTime created;
  private LocalDateTime modified;

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Artwork artwork)) return false;
    if (this == o) return true;
    if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    return id != null && Objects.equals(id, artwork.id);
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
