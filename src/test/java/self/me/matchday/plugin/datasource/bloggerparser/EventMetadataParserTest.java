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

package self.me.matchday.plugin.datasource.bloggerparser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import self.me.matchday.model.*;
import self.me.matchday.util.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventMetadataParserTest {

  private static final String LOG_TAG = "EventMetadataParserTest";

  private static final String TEST_TITLE =
      "La Liga 20/21 - Matchday 5 - Barcelona vs Sevilla - 04/10/2020";
  private static Event testEvent;

  @BeforeAll
  static void setUp() {

    BloggerParserPatterns bloggerParserPatterns = new BloggerParserPatterns() {};
    bloggerParserPatterns.competition = "^([\\w\\s])+ ";
    bloggerParserPatterns.fixture = "((Semi-)?Final)|((J|(Matchday ))\\d+)";
    bloggerParserPatterns.teams = "(?U)([\\w ?]+) vs.? ([\\w ?]+)";
    bloggerParserPatterns.date = "[\\w-\\/\\s]* - (\\d{2}/\\d{2}/\\d{4})$";
    bloggerParserPatterns.season = "(\\d{2,4})/(\\d{2,4})";

    EventMetadataParser eventMetadataParser = new EventMetadataParser();
    eventMetadataParser.setBloggerParserPatterns(bloggerParserPatterns);

    testEvent = eventMetadataParser.getEvent(TEST_TITLE);
    Log.i(LOG_TAG, "Testing Event: " + testEvent);

    assertThat(testEvent).isNotNull();
  }

  @Test
  @DisplayName("Validate parses competition")
  void testCompetition() {

    final Competition actualCompetition = testEvent.getCompetition();
    final Competition expectedCompetition = new Competition("La Liga");

    Log.i(LOG_TAG, "Testing parsed competition: " + actualCompetition);
    assertThat(actualCompetition).isEqualTo(expectedCompetition);
  }

  @Test
  @DisplayName("Validate parses season")
  void testSeason() {

    final Season actualSeason = testEvent.getSeason();
    final Season expectedSeason = new Season(2020, 2021);

    Log.i(LOG_TAG, "Testing parsed season: " + actualSeason);
    assertThat(actualSeason).isEqualTo(expectedSeason);
  }

  @Test
  @DisplayName("Validate parses fixture")
  void testFixture() {

    final Fixture actualFixture = testEvent.getFixture();
    final Fixture expectedFixture = new Fixture("Matchday", 5);

    Log.i(LOG_TAG, "Testing event fixture: " + actualFixture);
    assertThat(actualFixture).isEqualTo(expectedFixture);
  }

  @Test
  @DisplayName("Validate parses teams")
  void testTeams() {

    assertThat(testEvent).isInstanceOf(Match.class);
    final Match testMatch = (Match) testEvent;

    final Team actualHomeTeam = testMatch.getHomeTeam();
    final Team expectedHomeTeam = new Team("Barcelona");
    final Team actualAwayTeam = testMatch.getAwayTeam();
    final Team expectedAwayTeam = new Team("Sevilla");

    Log.i(
        LOG_TAG,
        String.format("Testing teams: home [%s] & away [%s]", actualHomeTeam, actualAwayTeam));
    assertThat(actualHomeTeam).isEqualTo(expectedHomeTeam);
    assertThat(actualAwayTeam).isEqualTo(expectedAwayTeam);
  }

  @Test
  @DisplayName("Validate parses date")
  void testDate() {

    final LocalDateTime actualDate = testEvent.getDate();
    final LocalDate expectedDate = LocalDate.of(2020, 10, 4);

    Log.i(LOG_TAG, "Testing event date: " + actualDate);

    assertThat(actualDate.getYear()).isEqualTo(expectedDate.getYear());
    assertThat(actualDate.getMonth()).isEqualTo(expectedDate.getMonth());
    assertThat(actualDate.getDayOfMonth()).isEqualTo(expectedDate.getDayOfMonth());
  }
}
