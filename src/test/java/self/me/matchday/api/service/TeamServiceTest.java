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
@DisplayName("Testing for Team service")
class TeamServiceTest {

  private static final String LOG_TAG = "TeamServiceTest";

  private static TeamService teamService;
  private static Competition testCompetition;
  private static Team testTeam;
  private static Match testMatch;
  private static EventService eventService;
  private static CompetitionService competitionService;

  @BeforeAll
  static void setUp(
      @Autowired final TeamService teamService,
      @Autowired final EventService eventService,
      @Autowired final CompetitionService competitionService) {

    TeamServiceTest.teamService = teamService;
    TeamServiceTest.eventService = eventService;
    TeamServiceTest.competitionService = competitionService;

    // Create test data
    final Match match = CreateTestData.createTestMatch();
    TeamServiceTest.eventService.saveEvent(match);

    // get Spring managed copy
    final Optional<Event> testEventOptional = eventService.fetchById(match.getEventId());
    assertThat(testEventOptional).isPresent();

    TeamServiceTest.testMatch = (Match) testEventOptional.get();
    testCompetition = testMatch.getCompetition();
    testTeam = testMatch.getHomeTeam();

    // Save test data
    TeamServiceTest.teamService.saveTeam(testTeam);
  }

  @AfterAll
  static void tearDown() {

    // delete test data
    eventService.deleteEvent(testMatch);
    teamService.deleteTeamById(testTeam.getTeamId());
    competitionService.deleteCompetitionById(testCompetition.getCompetitionId());
  }

  @Test
  @DisplayName("Validate retrieval of all teams fromm database")
  void fetchAllTeams() {

    final int expectedTeamCount = 1;

    final Optional<List<Team>> teamsOptional = teamService.fetchAllTeams();
    assertThat(teamsOptional).isPresent();

    teamsOptional.ifPresent(
        teams -> {
          Log.i(LOG_TAG, "Got teams:\n" + teams);
          assertThat(teams.size()).isGreaterThanOrEqualTo(expectedTeamCount);
          assertThat(teams).contains(testTeam);
        });
  }

  @Test
  @DisplayName("Verify retrieval of specific team from database")
  void fetchTeamById() {

    final Optional<Team> teamOptional = teamService.fetchTeamById(testTeam.getTeamId());
    assertThat(teamOptional).isPresent();

    teamOptional.ifPresent(
        team -> {
          Log.i(LOG_TAG, "Got team: " + team);
          assertThat(team).isEqualTo(testTeam);
        });
  }

  @Test
  @DisplayName("Validate retrieval of teams for a given Competition")
  void fetchTeamsByCompetitionId() {

    final Optional<List<Team>> teamsOptional =
        teamService.fetchTeamsByCompetitionId(testCompetition.getCompetitionId());
    assertThat(teamsOptional).isPresent();

    teamsOptional.ifPresent(
        teams -> {
          Log.i(
              LOG_TAG,
              String.format("Found %s teams for Competition: %s", teams.size(), testCompetition));
          assertThat(teams).contains(testTeam);
        });
  }

  @Test
  @DisplayName("Validate saving team to database")
  void saveTeam() {

    final Team savingTestTeam = new Team("Saving Test Team");

    final Optional<List<Team>> optionalTeams = teamService.fetchAllTeams();
    assertThat(optionalTeams).isPresent();

    final List<Team> teams = optionalTeams.get();
    final int initialTeamCount = teams.size();

    // Save team to database
    teamService.saveTeam(savingTestTeam);
    // Get new team list
    final Optional<List<Team>> optionalTeamsPostUpdate = teamService.fetchAllTeams();
    assertThat(optionalTeamsPostUpdate).isPresent();
    final List<Team> postUpdateTeams = optionalTeamsPostUpdate.get();
    final int postUpdateTeamCount = postUpdateTeams.size();

    assertThat(postUpdateTeamCount).isGreaterThan(initialTeamCount);
    assertThat(postUpdateTeamCount - initialTeamCount).isEqualTo(1);

    // Cleanup
    teamService.deleteTeamById(savingTestTeam.getTeamId());
  }

  @Test
  @DisplayName("Validate team deletion")
  void deleteTeamById() {

    final Team deleteTestTeam = new Team("Delete Test Team");
    // Save to database
    teamService.saveTeam(deleteTestTeam);

    // Get team count
    final Optional<List<Team>> optionalTeams = teamService.fetchAllTeams();
    assertThat(optionalTeams).isPresent();
    final List<Team> updatedTeams = optionalTeams.get();
    final int updatedTeamCount = updatedTeams.size();

    // Delete test team
    teamService.deleteTeamById(deleteTestTeam.getTeamId());
    // Get new team count
    final Optional<List<Team>> deletedTeamsListOptional = teamService.fetchAllTeams();
    assertThat(deletedTeamsListOptional).isPresent();
    final List<Team> deletedTeamsList = deletedTeamsListOptional.get();
    final int deletedTeamsCount = deletedTeamsList.size();

    assertThat(updatedTeamCount).isGreaterThan(deletedTeamsCount);
    assertThat(updatedTeamCount - deletedTeamsCount).isEqualTo(1);
  }
}
