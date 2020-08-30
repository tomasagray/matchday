/*
 * Copyright (c) 2020.
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

import java.io.Serializable;
import java.util.Locale;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Abbreviator;

/**
 * Represents a football team.
 *
 * @author tomas
 */
@Data
@Entity
@Table(name = "Teams")
public class Team implements Serializable {

  private static final long serialVersionUID = 123456L; // for serialization across platforms

  // Fields
  @Id
  private final String teamId;
  private String name;
  private String abbreviation;
  private Locale locale;
  @OneToOne
  private Artwork emblem;
  @OneToOne
  private Artwork fanart;

  // Default constructor
  public Team() {
    this.teamId = MD5String.generate();
  }

  public Team(@NotNull String name) {
    this.name = name;
    // Defaults
    this.abbreviation = Abbreviator.abbreviate(this.name);
    this.teamId = MD5String.fromData(name);
  }

  @Override
  public String toString() {
    return this.name;
  }

  @Override
  public int hashCode() {
    return name.hashCode() * teamId.hashCode();
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
    } else if (!(obj instanceof Team)) {
      return false;
    }

    // Cast for comparison
    final Team team = (Team) obj;
    return team.getName().equals(this.getName()) && team.getLocale().equals(this.getLocale());
  }
}
