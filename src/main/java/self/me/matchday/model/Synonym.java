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

package self.me.matchday.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
@Entity
public class Synonym {

  @ElementCollection(fetch = FetchType.EAGER)
  private final List<String> synonyms;

  @Id @GeneratedValue private Long id;
  private int primaryIndex = 0;

  public Synonym() {
    this.synonyms = new ArrayList<>();
  }

  public void add(@NotNull String synonym) {
    this.synonyms.add(synonym);
  }

  public boolean del(@NotNull String synonym) {
    return this.synonyms.remove(synonym);
  }

  @NotNull
  public String getPrimary() {
    return synonyms.get(primaryIndex);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    Synonym synonym = (Synonym) o;
    return id != null && Objects.equals(id, synonym.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this);
  }
}
