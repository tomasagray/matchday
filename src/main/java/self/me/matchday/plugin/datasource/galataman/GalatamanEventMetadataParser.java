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

package self.me.matchday.plugin.datasource.galataman;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.Highlight.HighlightBuilder;
import self.me.matchday.model.Match;
import self.me.matchday.model.Season;
import self.me.matchday.model.Team;
import self.me.matchday.plugin.datasource.EventMetadataParser;
import self.me.matchday.util.BeanLocator;
import self.me.matchday.util.Log;

/**
 * Extract Event metadata (e.g., Teams, Competition, Fixture, etc.) from Galataman data.
 */
public class GalatamanEventMetadataParser implements EventMetadataParser {

  private static final String LOG_TAG = "GalatamanEventMetadataParser";
  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final GManPatterns gManPatterns;
  // Metadata
  private final String title;
  private final Competition competition;
  private final Season season;
  private final Fixture fixture;
  private final LocalDateTime date;
  // Teams - only for a Match
  private final boolean hasTeams;
  private Team homeTeam;
  private Team awayTeam;

  public GalatamanEventMetadataParser(@NotNull final String title) {

    // Get patterns instance
    this.gManPatterns = BeanLocator.getBean(GManPatterns.class);

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
      return new HighlightBuilder()
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
    final Matcher matcher = gManPatterns.getCompetitionMatcher(title);
    return
        matcher.find() ? new Competition(matcher.group().trim()) : null;
  }

  private @NotNull Season parseSeasonData() {

    Season result = new Season();
    final Matcher matcher = gManPatterns.getSeasonMatcher(title);
    try {
      if (matcher.find()) {
        final int startYear = fixYear(Integer.parseInt(matcher.group(1)));
        final int endYear = fixYear(Integer.parseInt(matcher.group(2)));
        result = new Season(startYear, endYear);
      }
    } catch (NumberFormatException ignore) {}

    return result;
  }

  private @NotNull Fixture parseFixtureData() {

    // Result container
    Fixture result = new Fixture();
    final Matcher matcher = gManPatterns.getFixtureMatcher(title);

    try {
      if (matcher.find()) {
        if (matcher.group(1) != null) {
          result = new Fixture(matcher.group(1));
        } else if (matcher.group(3) != null && matcher.group(4) != null) {
          final String str = matcher.group(3).replace(matcher.group(4), "");
          final int i = Integer.parseInt(str);
          result = new Fixture(i);
        }
      }
    } catch (NumberFormatException ignore) {}
    return result;
  }

  private @NotNull LocalDateTime parseDateData() {

    // Result container
    LocalDateTime result = LocalDateTime.now();
    try {
      final Matcher dateMatcher = gManPatterns.getDateMatcher(title);
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
    final Matcher teamMatcher = gManPatterns.getTeamsMatcher(title);
    if (teamMatcher.find()) {
      this.homeTeam = new Team(teamMatcher.group(1).trim());
      this.awayTeam = new Team(teamMatcher.group(2).trim());
      return true;
    } else {
      return false;
    }
  }
}
