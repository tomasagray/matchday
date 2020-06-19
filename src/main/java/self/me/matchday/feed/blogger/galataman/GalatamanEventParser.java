/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed.blogger.galataman;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import self.me.matchday.feed.IEventParser;
import self.me.matchday.feed.InvalidMetadataException;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.HighlightShow;
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
  private static final Pattern COMP_PATTERN = Pattern.compile("^(\\w+[^0-9])+ ");
  private static final Pattern SEASON_PATTERN = Pattern.compile("\\d{2}/\\d{2}");
  private static final Pattern FIXTURE_PATTERN = Pattern.compile("(Matchday) (\\d+)");
  private static final Pattern TEAMS_PATTERN = Pattern.compile("(?U)([\\w ?]+) vs.? ([\\w ?]+)");
  private static final Pattern DATE_PATTERN = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

  // Fields
  private final String title;
  private final Competition competition;
  private final Season season;
  private final Fixture fixture;
  private final LocalDateTime date;
  // Teams - only for a Match
  private final boolean hasTeams;
  private Team homeTeam;
  private Team awayTeam;

  public GalatamanEventParser(@NotNull final String title) {

    this.title = title;

    // Determine each element of the Event metadata from title parts.
    this.competition = parseCompetitionData();
    this.season = parseSeasonData();
    this.fixture = parseFixtureData();
    this.date = parseDateData();
    // Determine if Teams are present
    this.hasTeams = setupTeams();
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
          .setTitle(title)
          .setDate(date)
          .build();
    }
  }

  private @Nullable Competition parseCompetitionData() {

    // Get "competition" substring
    final Matcher matcher = COMP_PATTERN.matcher(title);
    return
        matcher.find() ? new Competition(matcher.group().trim()) : null;
  }

  private @NotNull Season parseSeasonData() {

    final int MILLENNIUM = 2_000;
    int startYear = MILLENNIUM, endYear = MILLENNIUM;
    final String SEASON_SPLITTER = "/";

    // Parse season
    final Matcher seasonMatcher = SEASON_PATTERN.matcher(title);
    if (seasonMatcher.find()) {
      // Get season String
      final String seasonString = seasonMatcher.group().trim();
      // Parse season data
      if (seasonString.contains(SEASON_SPLITTER)) {
        final String[] years = seasonString.split(SEASON_SPLITTER);
        // Ensure we have exactly 2 years
        if (years.length != 2) {
          throw new InvalidMetadataException(
              String.format("Could not parse Season years from title: %s", title));
        }
        // Convert Strings to ints
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

  private @Nullable Fixture parseFixtureData() {

    // Parse fixture
    final Matcher fixtureMatcher = FIXTURE_PATTERN.matcher(title);
    return
        fixtureMatcher.find() ?
            new Fixture(fixtureMatcher.group(1), Integer.parseInt(fixtureMatcher.group(2))) :
            null;
  }

  private @NotNull LocalDateTime parseDateData() {

    // Result container
    LocalDateTime result = LocalDateTime.now();
    try {
      final Matcher dateMatcher = DATE_PATTERN.matcher(title);
      if (dateMatcher.find()) {
        result =
            LocalDate
                .parse(dateMatcher.group(), DATE_TIME_FORMATTER)
                .atStartOfDay();
      }
    } catch (DateTimeParseException | IllegalStateException e) {
      Log.d(
          LOG_TAG,
          String.format(
              "Could not parse Event data from GalatamanPost: [%s]; defaulting to current datetime",
              title), e
      );
    }
    return result;
  }

  private boolean setupTeams() {

    // Determine if there are teams
    final Matcher teamMatcher = TEAMS_PATTERN.matcher(title);
    if (teamMatcher.find()) {
      this.homeTeam = new Team(teamMatcher.group(1).trim());
      this.awayTeam = new Team(teamMatcher.group(2).trim());
      return true;
    } else {
      return false;
    }
  }
}
