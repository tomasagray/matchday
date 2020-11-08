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

package self.me.matchday.api.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Match;
import self.me.matchday.model.Team;
import self.me.matchday.util.Log;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for Match service")
class MatchServiceTest {

  private static final String LOG_TAG = "MatchServiceTest";

  private static MatchService matchService;
  private static Competition testCompetition;
  private static Team testTeam;
  private static Match testMatch;
  private static EventService eventService;

  @BeforeAll
  static void setUp(@Autowired final MatchService matchService, @Autowired final EventService eventService) {

    MatchServiceTest.matchService = matchService;
    MatchServiceTest.eventService = eventService;

    testCompetition = new Competition("TEST COMPETITION");
    testTeam = new Team("TEST TEAM");
    testMatch =
            new Match.MatchBuilder()
                    .setDate(LocalDateTime.now())
                    .setCompetition(testCompetition)
                    .setHomeTeam(testTeam)
                    .build();
    eventService.saveEvent(testMatch);
  }

  @Test
  @DisplayName("Test fetching all Matches from database")
  void fetchAllMatches() {

    final int expectedMatchCount = 1;  // minimum
    final Optional<List<Match>> optionalMatches = matchService.fetchAllMatches();
    assertThat(optionalMatches).isPresent();

    optionalMatches.ifPresent(
        matches -> {
          final int actualMatchCount = matches.size();
          Log.i(LOG_TAG, String.format("Found: %s Matches", actualMatchCount));
          assertThat(actualMatchCount).isGreaterThanOrEqualTo(expectedMatchCount);
        });
  }

  @Test
  @DisplayName("Test retrieval of a particular Match from database")
  void fetchMatch() {

    final String testMatchId = testMatch.getEventId();
    Log.i(LOG_TAG, "Attempting to retrieve Match from database; ID: " + testMatchId);
    final Optional<Match> optionalMatch = matchService.fetchMatch(testMatchId);

    assertThat(optionalMatch).isPresent();
    optionalMatch.ifPresent(match -> {
      Log.i(LOG_TAG, "Got match: " + match);
      assertThat(match.getHomeTeam()).isEqualTo(testTeam);
      assertThat(match.getCompetition()).isEqualTo(testCompetition);
    });
  }

  @AfterAll
  static void tearDown() {
    // delete test event
    eventService.deleteEvent(testMatch);
  }
}