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

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Abbreviator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a football team.
 *
 * @author tomas
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Team implements Serializable {

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  private UUID teamId;

  private String name;
  private String abbreviation;
  private Locale locale;
  @OneToOne private Artwork emblem;
  @OneToOne private Artwork fanart;

  public Team(@NotNull String name) {
    this.name = name;
    this.abbreviation = Abbreviator.abbreviate(this.name);
  }

  @Override
  public String toString() {
    return this.name;
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
    return Objects.hash(teamId, name, abbreviation, locale, emblem, fanart);
  }
}
