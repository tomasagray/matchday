/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed.blogger.galataman;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.IEventDataParser;
import self.me.matchday.feed.EventSource;
import self.me.matchday.feed.InvalidMetadataException;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.HighlightShow;
import self.me.matchday.model.Match;
import self.me.matchday.model.Season;
import self.me.matchday.model.Team;

/**
 * Extract Event metadata (e.g., Teams, Competition, Fixture, etc.) from Galataman data.
 */
public class GalatamanEventDataParser implements IEventDataParser {

  // Patterns
  private static final String TITLE_SPLITTER = Pattern.compile(" - ").pattern();
  private static final String SEASON_SPLITTER = Pattern.compile("/").pattern();
  private static final Pattern FIXTURE_PATTERN = Pattern.compile("\\d{2}");
  private static final Pattern TEAM_PATTERN = Pattern.compile(" vs.? ");
  private static final Pattern SEASON_PATTERN = Pattern.compile("\\d{2}/?\\d{2}");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("DD/mm/yyyy");

  // Constants
  private static final int MILLENNIUM = 2_000;
  // Title part indices
  private static final int COMP_SEASON_INDEX = 0;
  private static final int FIXTURE_INDEX = 1;
  private static final int TEAM_INDEX = 2;

  // Fields
  private final String[] titleParts;
  private final Competition competition;
  private final Season season;
  private final Fixture fixture;
  private final LocalDateTime date;
  // Teams - Match
  private final boolean hasTeams;
  private Team homeTeam;
  private Team awayTeam;

  GalatamanEventDataParser(@NotNull EventSource eventSource) {
    // Cast to Galataman Post
    GalatamanPost galatamanPost = (GalatamanPost) eventSource;
    this.titleParts = galatamanPost.getTitle().split(TITLE_SPLITTER);
    this.competition = parseCompetitionData(titleParts[COMP_SEASON_INDEX]);
    this.season = parseSeasonData(titleParts[COMP_SEASON_INDEX]);
    this.fixture = parseFixtureData(titleParts[FIXTURE_INDEX]);
    this.date = galatamanPost.getPublished();
    // Determine if Teams are present
    this.hasTeams = setupTeams(titleParts[TEAM_INDEX]);
  }

  /**
   * Assemble and return an Event object of the appropriate subtype.
   *
   * @return An Event
   */
  @Override
  public Event getEvent() {
    // Determine Event type from presence of Teams
    if(hasTeams) {
      return new Match.MatchBuilder()
          .setHomeTeam(homeTeam)
          .setAwayTeam(awayTeam)
          .setCompetition(competition)
          .setSeason(season)
          .setFixture(fixture)
          .setDate(date)
          .build();
    } else {
      return new HighlightShow.HighlightShowBuilder()
          .setCompetition(competition)
          .setSeason(season)
          .setFixture(fixture)
          .setTitle(titleParts[TEAM_INDEX])
          .setDate(date)
          .build();
    }
  }

  @NotNull
  @Contract("_ -> new")
  private Competition parseCompetitionData(@NotNull String competitionSubstring) {

    // Parse competition
    final Matcher seasonMatcher = SEASON_PATTERN.matcher(competitionSubstring);

    if (seasonMatcher.find()) {
      // Get season String
      final String seasonString = seasonMatcher.group();
      // Competition title is minus Season data
      final String competitionTitle = competitionSubstring.replace(seasonString, "");
      // Create Competition
      return new Competition(competitionTitle);
    } else {
      // Competition title is full substring
      return new Competition(competitionSubstring);
    }
  }

  @NotNull
  @Contract("_ -> new")
  private Season parseSeasonData(@NotNull String seasonSubstring) {

    // Parse season
    int startYear, endYear;
    final Matcher seasonMatcher = SEASON_PATTERN.matcher(seasonSubstring);

    if (seasonMatcher.find()) {
      // Get season String
      final String seasonString = seasonMatcher.group();
      // Parse season data
      if (seasonString.contains(SEASON_SPLITTER)) {
        final String[] years = seasonString.split(SEASON_SPLITTER);
        // Ensure we have exactly 2 years
        if (years.length != 2) {
          throw new InvalidMetadataException(
              "Could not parse Season years from title: " + Arrays.toString(titleParts));
        }

        startYear = Integer.parseInt(years[0]) + MILLENNIUM;
        endYear = Integer.parseInt(years[1]) + MILLENNIUM;
      } else {
        startYear = Integer.parseInt(seasonString);
        endYear = startYear + 1;
      }

      // Create Season
      return new Season(startYear, endYear);

    } else {
      // Return a default Season
      return new Season();
    }
  }

  @NotNull
  @Contract("_ -> new")
  private Fixture parseFixtureData(@NotNull String fixtureSubstring) {

    // Parse fixture
    final Matcher fixtureTitleMatcher = FIXTURE_PATTERN.matcher(fixtureSubstring);

    if (fixtureTitleMatcher.find()) {
      // This is a parsable Fixture title
      final int fixtureNumber = Integer.parseInt(fixtureTitleMatcher.group());
      final String fixtureTitle = fixtureSubstring.substring(0, fixtureTitleMatcher.start());
      return new Fixture(fixtureTitle, fixtureNumber);
    } else {
      return new Fixture(fixtureSubstring);
    }
  }

  private boolean setupTeams(@NotNull String teamSubstring) {

    // Determine if there are teams
    final Matcher teamMatcher = TEAM_PATTERN.matcher(teamSubstring);
    if (teamMatcher.find()) {
      // Home Team is first half of String
      final String homeTeamName = teamSubstring.substring(0, teamMatcher.start()).trim();
      // Away team is second half of String
      final String awayTeamName = teamSubstring.substring(teamMatcher.end()).trim();

      this.homeTeam = new Team(homeTeamName);
      this.awayTeam = new Team(awayTeamName);
      return true;
    } else {
      return false;
    }
  }
}
