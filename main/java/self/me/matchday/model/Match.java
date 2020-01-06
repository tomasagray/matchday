/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.Match.MatchId;

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
@IdClass(MatchId.class)
public final class Match extends Event implements Serializable {

  private static final long serialVersionUID = 123456L; // for cross-platform serialization

  @Id
  @ManyToOne(targetEntity = Team.class)
  @JoinColumn(name = "homeTeamId")
  private Team homeTeam;

  @Id
  @ManyToOne(targetEntity = Team.class)
  @JoinColumn(name = "awayTeamId")
  private Team awayTeam;

  @Id
  @ManyToOne(targetEntity = Competition.class)
  @JoinColumn(name = "competitionId")
  private Competition competition;

  @Id
  @ManyToOne(targetEntity = Season.class)
  @JoinColumn(name = "seasonId")
  private Season season;

  @Id
  @ManyToOne(targetEntity = Fixture.class)
  @JoinColumn(name = "fixtureId")
  private Fixture fixture;

  @Id
  @Column(name = "date")
  private LocalDateTime date;

  // Default constructor
  public Match() {}

  @Contract(pure = true)
  public Match(
      Team homeTeam,
      Team awayTeam,
      Competition competition,
      Season season,
      Fixture fixture,
      LocalDateTime date) {
    //    this.matchID = matchID;
    this.homeTeam = homeTeam;
    this.awayTeam = awayTeam;
    this.competition = competition;
    this.date = date;
    this.season = season;
    this.fixture = fixture;
  }

  @NotNull
  @Override
  public String toString() {
    String str =
        "Competition: " + competition + "\n" + "Season: " + season + "\n" + "Teams: " + homeTeam;
    if (awayTeam != null) {
      str += " vs. " + awayTeam;
    }
    str += "\n" + "Fixture: " + fixture + "\n";
    if (date != null) {
      str += "Date: " + DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(date);
    } else {
      str += "Date is NULL!";
    }

    return str;
  }

//  @Override
//  public boolean equals(Object obj) {
//    if (obj == this) {
//      return true;
//    }
//    if (!(obj instanceof Match)) {
//      return false;
//    }
//
//    // Cast for comparison
//    Match match = (Match) obj;
//    return match.getHomeTeam().equals(this.getHomeTeam())
//        && match.getAwayTeam().equals(this.getAwayTeam())
//        && match.getCompetition().equals(this.getCompetition())
//        && match.getSeason().equals(this.getSeason())
//        && match.getFixture().equals(this.getFixture())
//        && match.getDate().isEqual(this.getDate());
//  }
//
//  @Override
//  public int hashCode() {
//    return Objects.hash(this);
//  }

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

    public MatchBuilder setCompetition(Competition competition) {
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

  /** Class representing a unique ID for each Match object, based on the data within that object. */
  public static class MatchId implements Serializable {
    protected String homeTeam;
    protected String awayTeam;
    protected String competition;
    protected String season;
    protected String fixture;
    protected LocalDateTime date;

    @Contract(pure = true)
    public MatchId() {}

    @Contract(pure = true)
    public MatchId(
        String homeTeam,
        String awayTeam,
        String competition,
        String season,
        String fixture,
        LocalDateTime date) {
      this.homeTeam = homeTeam;
      this.awayTeam = awayTeam;
      this.competition = competition;
      this.season = season;
      this.fixture = fixture;
      this.date = date;
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (!(obj instanceof MatchId)) {
        return false;
      }

      MatchId matchId = (MatchId) obj;
      return matchId.homeTeam.equals(this.homeTeam)
          && matchId.awayTeam.equals(this.awayTeam)
          && matchId.competition.equals(this.competition)
          && matchId.season.equals(this.season)
          && matchId.fixture.equals(this.fixture)
          && matchId.date.equals(this.date);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this);
    }
  }
}
