/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

import java.awt.Image;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Class representing a match (game) between two teams (home & away) in a given Competition on a
 * specific date.
 *
 * @author tomas
 */
public class Match {
  // Fields
  // -----------------------------------------------------
  private MD5 matchID;
  // Match components
  private Team homeTeam;
  private Team awayTeam;
  private Competition competition;
  private LocalDate date;
  private Season season;
  private Fixture fixture;
  // Cover art
  private Image artwork;

  private Match( MD5 matchID, Team homeTeam, Team awayTeam,
      Competition competition, LocalDate date,  Season season,
     Fixture fixture) {
    this.matchID = matchID;
    this.homeTeam = homeTeam;
    this.awayTeam = awayTeam;
    this.competition = competition;
    this.date = date;
    this.season = season;
    this.fixture = fixture;
  }

  public MD5 getMatchID() {
    return matchID;
  }

  public Team getHomeTeam() {
    return homeTeam;
  }

  public Team getAwayTeam() {
    return awayTeam;
  }

  public Competition getCompetition() {
    return competition;
  }

  public LocalDate getDate() {
    return date;
  }

  public Season getSeason() {
    return season;
  }

  public Fixture getFixture() {
    return fixture;
  }

  public Image getArtwork() {
    return artwork;
  }

  public Match setMatchID(MD5 matchID) {
    this.matchID = matchID;
    return this;
  }

  public Match setHomeTeam(Team homeTeam) {
    this.homeTeam = homeTeam;
    return this;
  }

  public Match setAwayTeam(Team awayTeam) {
    this.awayTeam = awayTeam;
    return this;
  }

  public Match setCompetition(Competition competition) {
    this.competition = competition;
    return this;
  }

  public Match setDate(LocalDate date) {
    this.date = date;
    return this;
  }

  public Match setSeason(Season season) {
    this.season = season;
    return this;
  }

  public Match setFixture(Fixture fixture) {
    this.fixture = fixture;
    return this;
  }

  public Match setArtwork(Image artwork) {
    this.artwork = artwork;
    return this;
  }

  @Override
  public String toString() {
    String str =
        "Competition: " + competition + "\n"
            + "Season: " + season + "\n"
        + "Teams: " + homeTeam;
    if(awayTeam != null) {
      str += " vs. " + awayTeam.getName();
    }
    str += "\n"
          + "Fixture: " + fixture + "\n"
          + "Date: " + DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(date);

    return str;
  }

  /**
   * Builder class for Matches
   */
  public static class MatchBuilder {
    // Fields
    // -----------------------------------------------------
    private MD5 matchID;
    // Match components
    private Team homeTeam;
    private Team awayTeam;
    private Competition competition;
    private LocalDate date;
    private Season season;
    private Fixture fixture;
    // Cover art
    private Image artwork;

    public MatchBuilder setMatchID(MD5 matchID) {
      this.matchID = matchID;
      return this;
    }

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

    public MatchBuilder setDate(LocalDate date) {
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

    public MatchBuilder setArtwork(Image artwork) {
      this.artwork = artwork;
      return this;
    }

    public Match build() {
      Match match = new Match(matchID, homeTeam, awayTeam, competition, date, season, fixture);
      if(artwork != null) {
        match.setArtwork(artwork);
      }

      return match;
    }
  }
}
