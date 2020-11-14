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
import self.me.matchday.CreateTestData;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event;
import self.me.matchday.model.Match;
import self.me.matchday.model.Team;
import self.me.matchday.util.Log;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for Match service")
class MatchServiceTest {

  private static final String LOG_TAG = "MatchServiceTest";

  private static MatchService matchService;
  private static EventService eventService;
  private static CompetitionService competitionService;
  private static TeamService teamService;

  // Test data
  private static Competition testCompetition;
  private static Team testTeam;
  private static Match testMatch;

  @BeforeAll
  static void setUp(
      @Autowired final MatchService matchService,
      @Autowired final EventService eventService,
      @Autowired final CompetitionService competitionService,
      @Autowired final TeamService teamService) {

    MatchServiceTest.matchService = matchService;
    MatchServiceTest.eventService = eventService;
    MatchServiceTest.competitionService = competitionService;
    MatchServiceTest.teamService = teamService;

    final Match match = CreateTestData.createTestMatch();
    eventService.saveEvent(match);

    // Get managed copy
    final Optional<Event> testEventOptional = eventService.fetchById(match.getEventId());
    assertThat(testEventOptional).isPresent();

    MatchServiceTest.testMatch = (Match) testEventOptional.get();
    MatchServiceTest.testCompetition = testMatch.getCompetition();
    MatchServiceTest.testTeam = testMatch.getHomeTeam();

  }

  @Test
  @DisplayName("Test fetching all Matches from database")
  void fetchAllMatches() {

    final int expectedMatchCount = 1; // minimum
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
    optionalMatch.ifPresent(
        match -> {
          Log.i(LOG_TAG, "Got match: " + match);
          assertThat(match.getHomeTeam()).isEqualTo(testTeam);
          assertThat(match.getCompetition()).isEqualTo(testCompetition);
        });
  }

  @AfterAll
  static void tearDown() {

    // delete test event
    eventService.deleteEvent(testMatch);
    competitionService.deleteCompetitionById(testCompetition.getCompetitionId());
    teamService.deleteTeamById(testTeam.getTeamId());
  }
}
