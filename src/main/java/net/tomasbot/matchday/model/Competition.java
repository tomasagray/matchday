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

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package net.tomasbot.matchday.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a competition, e.g., a domestic league (EPL) or cup (FA Cup), a tournament (UCL, World
 * Cup), etc.
 *
 * @author tomas
 */
@Getter
@Setter
@Entity
public class Competition implements Serializable {

  @OneToOne(cascade = CascadeType.ALL)
  private final ProperName name;

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Type(type = "uuid-char")
  private UUID id;

  @ManyToOne private Country country;

  // artwork
  @OneToOne(cascade = CascadeType.ALL)
  private ArtworkCollection emblem = new ArtworkCollection(ArtworkRole.EMBLEM);

  @OneToOne(cascade = CascadeType.ALL)
  private ArtworkCollection fanart = new ArtworkCollection(ArtworkRole.FANART);

  public Competition(@NotNull final String name) {
    this.name = new ProperName(name);
  }

  public Competition(@NotNull ProperName name) {
    this.name = name;
  }

  public Competition() {
    this.name = null;
  }

  @Override
  public String toString() {
    final ProperName properName = getName();
    final String name = properName == null ? "UNKNOWN" : properName.getName();
    return String.format("%s [%s]", name, getId());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Competition that)) return false;
    return Objects.equals(getId(), that.getId())
        && Objects.equals(getName(), that.getName())
        && Objects.equals(getCountry(), that.getCountry());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getName(), getCountry());
  }
}
