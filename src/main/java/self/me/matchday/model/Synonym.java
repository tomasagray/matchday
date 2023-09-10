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

import java.util.Objects;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@Entity
public class Synonym {

  @EmbeddedId
  @GenericGenerator(name = "synonym_id_gen", strategy = "self.me.matchday.db.SynonymIdGenerator")
  @GeneratedValue(generator = "synonym_id_gen")
  private Md5Id id;

  private final String name;

  public Synonym() {
    this.id = null;
    this.name = null;
  }

  public Synonym(@NotNull String name) {
    this.name = name.trim();
    this.id = new Md5Id(this.name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Synonym synonym)) return false;
    return Objects.equals(getId(), synonym.getId()) && Objects.equals(getName(), synonym.getName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getName());
  }

  @Override
  public String toString() {
    return String.format("Synonym{id=%s, name='%s'}", id, name);
  }
}
