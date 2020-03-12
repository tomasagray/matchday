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
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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

  // For external identification (MD5 string)
//  private final String matchId;

  @ManyToOne(targetEntity = Team.class, cascade = CascadeType.ALL)
  @JoinColumn(name = "homeTeamId")
  private Team homeTeam;

  @ManyToOne(targetEntity = Team.class, cascade = CascadeType.ALL)
  @JoinColumn(name = "awayTeamId")
  private Team awayTeam;

  // Default constructor
  public Match() {
    /*this.matchId = MD5String.generate();*/
  }

  @Contract(pure = true)
  public Match(
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
//    this.matchId = generateMatchId();
  }

  @NotNull
  @Override
  public String getTitle() {
    return competition.getName()
        + ": "
        + homeTeam.getName()
        + " vs. "
        + awayTeam.getName()
        + " - "
        + ((fixture.getFixtureNumber() != 0)
            ? String.format("%s %s", fixture.getTitle(), fixture.getFixtureNumber())
            : fixture.getTitle());
  }

  @NotNull
  @Override
  public String toString() {
    String str =
        "Competition: " + competition + ", " + "Season: " + season + ", " + "Teams: " + homeTeam;
    if (awayTeam != null) {
      str += " vs. " + awayTeam;
    }
    str += ", " + "Fixture: " + fixture + ", ";
    if (date != null) {
      str += "Date: " + DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(date);
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

  @NotNull
  private String generateMatchId() {

    return MD5String.fromData(
        this.homeTeam.getTeamId()
            + this.awayTeam.getTeamId()
            + this.competition.getCompetitionId()
            + this.getSeason().getSeasonId()
            + this.getFixture().getFixtureId()
            + this.getDate().format(EVENT_ID_DATE_FORMATTER));
  }
}
