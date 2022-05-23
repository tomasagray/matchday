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
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
public class ProperName implements Comparable<ProperName> {

  private final String name;
  @Id @GeneratedValue private Long id;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private List<Synonym> synonyms = new ArrayList<>();

  public ProperName() {
    this.name = null;
  }

  public ProperName(@NotNull String name) {
    this.name = name;
  }

  public void addSynonym(@NotNull Synonym synonym) {
    this.synonyms.add(synonym);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ProperName)) return false;
    ProperName that = (ProperName) o;
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
