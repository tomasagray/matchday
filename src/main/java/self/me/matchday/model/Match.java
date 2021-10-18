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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Class representing a match (game) between two teams (home & away) in a given Competition on a
 * specific date.
 *
 * @author tomas
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "Matches")
public class Match extends Event implements Serializable {

  private static final long serialVersionUID = 123456L; // for cross-platform serialization

  @ManyToOne(targetEntity = Team.class, cascade = CascadeType.MERGE)
  @JoinColumn(name = "home_team_id")
  private Team homeTeam;

  @ManyToOne(targetEntity = Team.class, cascade = CascadeType.MERGE)
  @JoinColumn(name = "away_team_id")
  private Team awayTeam;

  // Default constructor
  public Match() {
    super();
    this.eventId = MD5String.generate();
  }

  @Contract(pure = true)
  private Match(
      Team homeTeam,
      Team awayTeam,
      Competition competition,
      Season season,
      Fixture fixture,
      LocalDateTime date) {
    this.homeTeam = homeTeam;
    this.awayTeam = awayTeam;
    this.competition = competition;
    this.date = date;
    this.season = season;
    this.fixture = fixture;
    this.eventId = MD5String.fromData(homeTeam, awayTeam, competition, date, season, fixture);
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

  @NotNull
  @Override
  public String toString() {
    String str =
        "Competition: "
            + getCompetition()
            + ", "
            + "Season: "
            + getSeason()
            + ", "
            + "Teams: "
            + getHomeTeam()
            + " vs. "
            + getAwayTeam()
            + ", "
            + "Fixture: "
            + getFixture()
            + ", ";
    if (date != null) {
      str += "Date: " + DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(getDate());
    } else {
      str += "Date is NULL!";
    }
    return str;
  }

  /** Builder class for Matches */
  public static class MatchBuilder {

    // Match components
    private Team homeTeam;
    private Team awayTeam;
    private Competition competition;
    private Season season;
    private Fixture fixture;
    private LocalDateTime date;

    public MatchBuilder setHomeTeam(Team homeTeam) {
      this.homeTeam = homeTeam;
      return this;
    }

    public MatchBuilder setAwayTeam(Team awayTeam) {
      this.awayTeam = awayTeam;
      return this;
    }

    public MatchBuilder setCompetition(@NotNull final Competition competition) {
      this.competition = competition;
      return this;
    }

    public MatchBuilder setDate(LocalDateTime date) {
      this.date = date;
      return this;
    }

    public MatchBuilder setSeason(Season season) {
      this.season = season;
      return this;
    }

    public MatchBuilder setFixture(Fixture fixture) {
      this.fixture = fixture;
      return this;
    }

    public Match build() {
      return new Match(homeTeam, awayTeam, competition, season, fixture, date);
    }
  }
}
