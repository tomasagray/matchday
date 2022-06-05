/*
 * Copyright (c) 2022.
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Match;
import self.me.matchday.model.Team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for Match service")
class MatchServiceTest {

  private static final Logger logger = LogManager.getLogger(MatchServiceTest.class);

  private static TestDataCreator testDataCreator;
  private static MatchService matchService;

  // Test data
  private static Competition testCompetition;
  private static Team testTeam;
  private static Match testMatch;

  @BeforeAll
  static void setUp(
      @Autowired final TestDataCreator testDataCreator,
      @Autowired final MatchService matchService) {

    MatchServiceTest.testDataCreator = testDataCreator;
    MatchServiceTest.matchService = matchService;

    // setup resources
    MatchServiceTest.testMatch = testDataCreator.createTestMatch();
    MatchServiceTest.testCompetition = testMatch.getCompetition();
    MatchServiceTest.testTeam = testMatch.getHomeTeam();
  }

  @AfterAll
  static void tearDown() {
    // delete test event
    testDataCreator.deleteTestEvent(testMatch);
  }

  @Test
  @DisplayName("Test fetching all Matches from database")
  void fetchAllMatches() {

    final int expectedMatchCount = 1; // minimum
    final List<Match> matches = matchService.fetchAll();

    final int actualMatchCount = matches.size();
    logger.info("Found: {} Matches", actualMatchCount);
    assertThat(actualMatchCount).isGreaterThanOrEqualTo(expectedMatchCount);
  }

  @Test
  @DisplayName("Test retrieval of a particular Match from database")
  void fetchMatch() {

    final UUID testMatchId = testMatch.getEventId();
    logger.info("Attempting to retrieve Match from database; ID: {}", testMatchId);
    final Optional<Match> optionalMatch = matchService.fetchById(testMatchId);

    assertThat(optionalMatch).isPresent();
    optionalMatch.ifPresent(
        match -> {
          logger.info("Got match: {}", match);
          assertThat(match.getHomeTeam()).isEqualTo(testTeam);
          assertThat(match.getCompetition()).isEqualTo(testCompetition);
        });
  }

  @Test
  @DisplayName("Ensure fetches all Matches for specified Team")
  void fetchMatchesForTeam() {

    // Minimum expected Events
    final int expectedEventCount = 1;
    logger.info("All Matches in database:\n{}", matchService.fetchAll());

    logger.info("Fetching Matches for Team: {}", testTeam);
    final List<Match> events = matchService.fetchMatchesForTeam(testTeam.getTeamId());
    logger.info("Got Matches:\n{}", events);
    assertThat(events.size()).isGreaterThanOrEqualTo(expectedEventCount);
  }
}
