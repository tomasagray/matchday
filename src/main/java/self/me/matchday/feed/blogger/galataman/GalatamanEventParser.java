/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed.blogger.galataman;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.InvalidMetadataException;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.HighlightShow;
import self.me.matchday.feed.IEventParser;
import self.me.matchday.model.Match;
import self.me.matchday.model.Season;
import self.me.matchday.model.Team;
import self.me.matchday.util.Log;

/**
 * Extract Event metadata (e.g., Teams, Competition, Fixture, etc.) from Galataman data.
 */
public class GalatamanEventParser implements IEventParser {

  private static final String LOG_TAG = "GalatamanEventParser";

  // Patterns
  private static final String TITLE_SPLITTER = Pattern.compile(" - ").pattern();
  private static final String SEASON_SPLITTER = Pattern.compile("/").pattern();
  private static final Pattern FIXTURE_PATTERN = Pattern.compile("\\d{2}");
  private static final Pattern TEAM_PATTERN = Pattern.compile(" vs.? ");
  private static final Pattern SEASON_PATTERN = Pattern.compile("\\d{2}/?\\d{2}");

  // Title part indices
  private static final int COMP_SEASON_INDEX = 0;
  private static final int FIXTURE_INDEX = 1;
  private static final int TEAM_INDEX = 2;
  private static final int DATE_INDEX = 3;

  // Fields
  private final String[] titleParts;
  private Competition competition;
  private Season season;
  private Fixture fixture;
  private LocalDateTime date;
  // Teams - Match
  private boolean hasTeams;
  private Team homeTeam;
  private Team awayTeam;

  public GalatamanEventParser(@NotNull final String title) {
    this.titleParts = title.split(TITLE_SPLITTER);
    parseEventMetadata();
  }

  /**
   * Determine each element of the Event metadata from title parts.
   */
  private void parseEventMetadata() {
    this.competition = parseCompetitionData(titleParts[COMP_SEASON_INDEX]);
    this.season = parseSeasonData(titleParts[COMP_SEASON_INDEX]);
    this.fixture = parseFixtureData(titleParts[FIXTURE_INDEX]);
    this.date = parseDateData(titleParts[DATE_INDEX]);
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
    if (hasTeams) {
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

    final int MILLENNIUM = 2_000;
    int startYear = MILLENNIUM, endYear = MILLENNIUM;
    final Matcher seasonMatcher = SEASON_PATTERN.matcher(seasonSubstring);

    // Parse season
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

        startYear += Integer.parseInt(years[0]);
        endYear += Integer.parseInt(years[1]);
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

  private LocalDateTime parseDateData(@NotNull final String dateSubstring) {

    // Container
    LocalDateTime result = LocalDateTime.now();
    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    try {
      result = LocalDate.parse(dateSubstring, dateTimeFormatter).atStartOfDay();
    } catch (DateTimeParseException e) {
      Log.d(
          LOG_TAG,
          String.format(
              "Could not parse Event data from GalatamanPost: [%s]; defaulting to current date",
              dateSubstring), e
      );
    }

    return result;
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
