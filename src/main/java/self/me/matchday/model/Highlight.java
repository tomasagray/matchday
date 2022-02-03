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

package self.me.matchday.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/** A highlight show, week in review or other non-Match televised Event. */
@Getter
@Setter
@Entity
public class Highlight extends Event implements Serializable {

  public Highlight() {
    super();
  }

  @Builder(builderMethodName = "highlightBuilder")
  public Highlight(
      UUID eventId, Competition competition, Season season, Fixture fixture, LocalDateTime date) {
    super(eventId, competition, null, null, season, fixture, date);
  }

  // Overrides
  @Override
  public @NotNull String toString() {
    return getTitle()
        + " ("
        + getCompetition()
        + " "
        + getSeason().getStartDate().getYear()
        + "/"
        + getSeason().getEndDate().getYear()
        + "), "
        + getFixture();
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Highlight)) {
      return false;
    }
    // Cast for comparison
    final Highlight highlight = (Highlight) o;
    return this.getEventId().equals(highlight.getEventId())
        && this.getCompetition().equals(highlight.getCompetition())
        && this.getFixture().equals(highlight.getFixture())
        && this.getSeason().equals(highlight.getSeason());
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + eventId.hashCode();
    hash = 31 * hash + ((competition != null) ? competition.hashCode() : 0);
    hash = 31 * hash + ((fixture != null) ? fixture.hashCode() : 0);
    hash = 31 * hash + ((season != null) ? season.hashCode() : 0);
    return hash;
  }
}
