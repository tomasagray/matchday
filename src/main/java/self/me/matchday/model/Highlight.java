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

package self.me.matchday.model;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/** A highlight show, week in review or other non-Match televised Event. */
@Data
@Entity
@Table(name = "Highlights")
public class Highlight extends Event implements Serializable {

  public Highlight() {
    this.eventId = MD5String.generate();
  }

  private Highlight(
      Competition competition, Season season, Fixture fixture, String title, LocalDateTime date) {
    this.competition = competition;
    this.season = season;
    this.fixture = fixture;
    this.title = title;
    this.date = date;
    this.eventId = MD5String.fromData(competition, season, fixture, title, date);
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

  /** A Builder class for this object. Returns a fully constructed Highlight object. */
  public static class HighlightBuilder {

    private Competition competition;
    private Season season;
    private Fixture fixture;
    private String title;
    private LocalDateTime date;

    public HighlightBuilder setCompetition(@NotNull Competition competition) {
      this.competition = competition;
      return this;
    }

    public HighlightBuilder setSeason(@NotNull Season season) {
      this.season = season;
      return this;
    }

    public HighlightBuilder setFixture(@NotNull Fixture fixture) {
      this.fixture = fixture;
      return this;
    }

    public HighlightBuilder setTitle(@NotNull String title) {
      this.title = title;
      return this;
    }

    public HighlightBuilder setDate(LocalDateTime date) {
      this.date = date;
      return this;
    }

    public Highlight build() {
      return new Highlight(this.competition, this.season, this.fixture, this.title, this.date);
    }
  }
}
