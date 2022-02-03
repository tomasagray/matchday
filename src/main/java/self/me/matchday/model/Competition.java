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
import java.util.UUID;

/**
 * Represents a competition, e.g., a domestic league (EPL) or cup (FA Cup), a tournament (UCL, World
 * Cup), etc.
 *
 * @author tomas
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Competition implements Serializable {

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  private UUID competitionId;

  private String name;
  private String abbreviation;
  private Locale locale;
  @OneToOne private Artwork emblem;
  @OneToOne private Artwork fanart;
  @OneToOne private Artwork monochromeEmblem;
  @OneToOne private Artwork landscape;

  public Competition(@NotNull final String name) {
    this(name, Abbreviator.abbreviate(name));
  }

  public Competition(@NotNull final String name, @NotNull final String abbreviation) {
    this.name = name;
    this.abbreviation = abbreviation;
  }

  @Override
  public String toString() {
    return this.name;
  }

  @Override
  public boolean equals(Object obj) {

    if (!(obj instanceof Competition)) {
      return false;
    }
    Competition competition = (Competition) obj;
    return competition.getName().equals(this.getName());
  }

  @Override
  public int hashCode() {
    return name.hashCode() * competitionId.hashCode();
  }
}
