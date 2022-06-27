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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import self.me.matchday.db.converter.PatternConverter;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Getter
@Setter
@ToString
@AllArgsConstructor
@Entity
public class PatternKit<T> {

  @Id @GeneratedValue private Long id;

  @Type(type = "java.lang.Class")
  private final Class<T> clazz;

  @Column(columnDefinition = "LONGTEXT")
  @Convert(converter = PatternConverter.class)
  private Pattern pattern;

  @ElementCollection(fetch = FetchType.EAGER)
  private Map<Integer, String> fields = new HashMap<>();

  public PatternKit(final Class<T> clazz) {
    this.clazz = clazz;
  }

  public PatternKit() {
    this(null);
  }
}
