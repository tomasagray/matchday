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

package net.tomasbot.matchday.model;

import java.util.*;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@Entity
public class ProperName implements Comparable<ProperName> {

  private final String name;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private Set<Synonym> synonyms = new HashSet<>();

  public ProperName() {
    this.name = null;
  }

  public ProperName(@NotNull String name) {
    this.name = name.trim();
  }

  public void addSynonym(@NotNull Synonym synonym) {
    this.synonyms.add(synonym);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ProperName that)) return false;
    return Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getName());
  }

  @Override
  public String toString() {
    return String.format("ProperName{id=%s, name='%s'}", id, name);
  }

  @Override
  public int compareTo(@NotNull ProperName o) {
    if (name == null) {
      if (o.getName() == null) {
        return 0;
      }
      return -1;
    }
    return name.compareTo(o.getName());
  }
}
