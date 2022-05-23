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
import java.util.Objects;

@Getter
@Setter
@Entity
public class Synonym {

  @Id @GeneratedValue private Long id;

  private final String name;

  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private final ProperName properName;

  public Synonym() {
    this.name = null;
    this.properName = null;
  }

  public Synonym(@NotNull String name, ProperName properName) {
    this.name = name;
    this.properName = properName;
    if (this.properName != null) {
      this.properName.addSynonym(this);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Synonym)) return false;
    Synonym synonym = (Synonym) o;
    return Objects.equals(getId(), synonym.getId())
        && Objects.equals(getName(), synonym.getName())
        && Objects.equals(getProperName(), synonym.getProperName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getName(), getProperName());
  }

  @Override
  public String toString() {
    return String.format("Synonym{id=%s, name='%s', properName=%s}", id, name, getProperName());
  }
}
