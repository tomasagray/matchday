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

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Class representing a match (game) between two teams (home & away) in a given Competition on a
 * specific date.
 *
 * @author tomas
 */
@Getter
@Setter
@Entity
public class Match extends Event {

  public Match() {
    super();
  }

  @Builder(builderMethodName = "matchBuilder")
  public Match(UUID eventId, Competition competition, Team homeTeam, Team awayTeam, Season season, Fixture fixture, LocalDateTime date) {
    super(eventId, competition, homeTeam, awayTeam, season, fixture, date);
  }

  @NotNull
  @Override
  public String getTitle() {

    return competition
        + ": "
        + homeTeam
        + " vs. "
        + awayTeam
        + ((fixture != null) ? ", " + fixture : "");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    Match match = (Match) o;
    return eventId != null && Objects.equals(eventId, match.eventId);
  }
}
