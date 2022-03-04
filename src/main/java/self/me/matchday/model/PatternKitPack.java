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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class PatternKitPack {

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private final Map<String, PatternKitEntry<?>> patternKits =
      new HashMap<>(); // todo - change String to Class<?>

  @Id @GeneratedValue private Long id;

  public void addAllPatternKits(@NotNull Collection<PatternKit<?>> patternKits) {
    patternKits.forEach(this::addPatternKit);
  }

  @SuppressWarnings("unchecked cast")
  public <T> void addPatternKit(@NotNull PatternKit<T> patternKit) {

    final Class<T> clazz = patternKit.getClazz();
    final PatternKitEntry<T> existingPatternKits =
        (PatternKitEntry<T>) patternKits.get(clazz.getName());
    if (existingPatternKits != null) {
      existingPatternKits.addPatternKit(patternKit);
    } else {
      final PatternKitEntry<T> patternKitEntry = new PatternKitEntry<>(clazz);
      patternKitEntry.addPatternKit(patternKit);
      patternKits.put(clazz.getName(), patternKitEntry);
    }
  }

  @SuppressWarnings("unchecked cast")
  public <T> List<PatternKit<? extends T>> getPatternKitsFor(@NotNull Class<T> clazz) {

    final PatternKitEntry<?> patternKitEntry = patternKits.get(clazz.getName());
    if (patternKitEntry != null) {
      return patternKitEntry.getPatternKits().stream()
          .map(t -> (PatternKit<T>) t)
          .collect(Collectors.toList());
    }
    return null; // not found
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    PatternKitPack that = (PatternKitPack) o;
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, patternKits);
  }
}
