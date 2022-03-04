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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@Entity
class PatternKitEntry<T> {

  @Type(type = "java.lang.Class")
  private final Class<T> clazz;

  @OneToMany(targetEntity = PatternKit.class, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private final List<PatternKit<T>> patternKits = new ArrayList<>();

  @Id @GeneratedValue private Long id;

  public PatternKitEntry(Class<T> clazz) {
    this.clazz = clazz;
  }

  public PatternKitEntry() {
    this.clazz = null;
  }

  @SuppressWarnings("unchecked cast")
  public void addPatternKit(@NotNull PatternKit<?> patternKit) {
    this.getPatternKits().add((PatternKit<T>) patternKit);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    PatternKitEntry<?> that = (PatternKitEntry<?>) o;
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, clazz, patternKits);
  }
}
