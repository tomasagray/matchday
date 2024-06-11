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

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a football team.
 *
 * @author tomas
 */
@Getter
@Setter
@Entity
public class Team {

  @OneToOne(cascade = CascadeType.ALL)
  private final ProperName name;

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Type(type = "uuid-char")
  private UUID id;

  @ManyToOne private Country country;

  @ElementCollection private List<Color> colors;

  @OneToOne(cascade = CascadeType.ALL)
  private ArtworkCollection emblem = new ArtworkCollection(ArtworkRole.EMBLEM);

  @OneToOne(cascade = CascadeType.ALL)
  private ArtworkCollection fanart = new ArtworkCollection(ArtworkRole.FANART);

  public Team(@NotNull ProperName name) {
    this.name = name;
  }

  public Team(@NotNull String name) {
    this.name = new ProperName(name);
  }

  public Team() {
    this.name = null;
  }

  @Override
  public String toString() {
    final ProperName properName = getName();
    final String name = properName == null ? "UNKNOWN" : properName.getName();
    return String.format("%s [%s]", name, getId());
  }

  /**
   * Compare Teams; they must have identical names and Locales.
   *
   * @param obj The team to be compared
   * @return True/false.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj == null) {
      return false;
    } else if (!(obj instanceof Team)) {
      return false;
    }

    // Cast for comparison
    final Team team = (Team) obj;
    return team.getName().equals(this.getName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, country, emblem, fanart);
  }
}
