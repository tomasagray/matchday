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

package self.me.matchday.plugin.datasource.bloggerparser;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import self.me.matchday.model.*;
import self.me.matchday.plugin.datasource.blogger.InvalidBloggerPostException;
import self.me.matchday.util.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EventMetadataParser {

  private static final String LOG_TAG = "EventMetadataParser";
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

  @Getter @Setter private BloggerParserPatterns bloggerParserPatterns;

  public Event getEvent(@NotNull final String title) {
    return parseEventFromTitle(title);
  }

  private Event parseEventFromTitle(@NotNull final String title) {

    // Determine each element of the Event metadata from title parts.
    final Competition competition = parseCompetitionData(title);
    final Season season = parseSeasonData(title);
    final Fixture fixture = parseFixtureData(title);
    final LocalDateTime date = parseDateData(title);

    // Determine if Teams are present
    final Matcher teamMatcher = bloggerParserPatterns.getTeamsMatcher(title);
    if (teamMatcher.find()) {
      final Team homeTeam = new Team(teamMatcher.group(1).trim());
      final Team awayTeam = new Team(teamMatcher.group(2).trim());
      return new Match.MatchBuilder()
          .setHomeTeam(homeTeam)
          .setAwayTeam(awayTeam)
          .setCompetition(competition)
          .setSeason(season)
          .setFixture(fixture)
          .setDate(date)
          .build();
    } else {
      return new Highlight.HighlightBuilder()
          .setCompetition(competition)
          .setSeason(season)
          .setFixture(fixture)
          .setTitle(title)
          .setDate(date)
          .build();
    }
  }

  private @NotNull Competition parseCompetitionData(@NotNull final String title) {

    // Get "competition" substring
    final Matcher matcher = bloggerParserPatterns.getCompetitionMatcher(title);
    if (matcher.find()) {
      return new Competition(matcher.group().trim());
    }
    // else...
    throw new InvalidBloggerPostException(
        String.format("%s could not parse title: %s", LOG_TAG, title));
  }

  private @NotNull Season parseSeasonData(@NotNull final String title) {

    Season result = new Season();
    final Matcher matcher = bloggerParserPatterns.getSeasonMatcher(title);
    try {
      if (matcher.find()) {
        final int startYear = fixYear(Integer.parseInt(matcher.group(1)));
        final int endYear = fixYear(Integer.parseInt(matcher.group(2)));
        result = new Season(startYear, endYear);
      }
    } catch (NumberFormatException ignore) {
    }

    return result;
  }

  private @NotNull Fixture parseFixtureData(@NotNull final String title) {

    // Result container
    Fixture result = new Fixture();
    final Matcher matcher = bloggerParserPatterns.getFixtureMatcher(title);

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
    } catch (NumberFormatException ignore) {
    }
    return result;
  }

  private @NotNull LocalDateTime parseDateData(@NotNull final String title) {

    // Result container
    LocalDateTime result = LocalDateTime.now();
    try {
      final Matcher dateMatcher = bloggerParserPatterns.getDateMatcher(title);
      if (dateMatcher.find()) {
        result = LocalDate.parse(dateMatcher.group(1), DATE_TIME_FORMATTER).atStartOfDay();
      }
    } catch (RuntimeException exception) {
      Log.e(
          LOG_TAG,
          String.format(
              "Could not parse Event date from post title: [%s]; defaulting to current datetime",
              title),
          exception);
    }
    return result;
  }

  /**
   * Correct year to YYYY format
   *
   * @param year The year to be fixed
   * @return The year in YYYY format
   */
  private int fixYear(final int year) {

    // Constants
    final int MILLENNIUM = 2_000;
    final int CENTURY = 100;
    final int CURRENT_YEAR = LocalDate.now().getYear() % CENTURY;

    if (year < CENTURY) {
      return year + MILLENNIUM;
    }
    // No changes necessary
    return year;
  }
}
