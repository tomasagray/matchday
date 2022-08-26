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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Match;
import self.me.matchday.model.Team;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("Testing for Team service")
class TeamServiceTest {

  private static final Logger logger = LogManager.getLogger(TeamServiceTest.class);

  private final TeamService teamService;
  private final Competition testCompetition;
  private final Team testTeam;

  @Autowired
  public TeamServiceTest(@NotNull TestDataCreator testDataCreator, TeamService teamService) {
    this.teamService = teamService;
    Match testMatch = testDataCreator.createTestMatch();
    this.testCompetition = testMatch.getCompetition();
    this.testTeam = testMatch.getHomeTeam();
  }

  @Test
  @DisplayName("Validate retrieval of all teams from database")
  void fetchAllTeams() {

    final int expectedTeamCount = 1;

    final List<Team> teams = teamService.fetchAll();
    logger.info("Got teams:\n" + teams);
    assertThat(teams.size()).isGreaterThanOrEqualTo(expectedTeamCount);
    assertThat(teams).contains(testTeam);
  }

  @Test
  @DisplayName("Verify retrieval of specific team from database")
  void fetchTeamById() {

    final Optional<Team> teamOptional = teamService.fetchById(testTeam.getTeamId());
    assertThat(teamOptional).isPresent();

    teamOptional.ifPresent(
        team -> {
          logger.info("Got team: " + team);
          assertThat(team).isEqualTo(testTeam);
        });
  }

  @Test
  @DisplayName("Validate retrieval of teams for a given Competition")
  void fetchTeamsByCompetitionId() {

    final List<Team> teams = teamService.fetchTeamsByCompetitionId(testCompetition.getId());
    logger.info("Found {} teams for Competition: {}", teams.size(), testCompetition);
    assertThat(teams).contains(testTeam);
  }

  @Test
  @DisplayName("Validate saving team to database")
  void saveTeam() {

    final Team savingTestTeam = new Team("Saving Test Team");

    final List<Team> teams = teamService.fetchAll();
    final int initialTeamCount = teams.size();

    // Save team to database
    final Team savedTeam = teamService.save(savingTestTeam);
    logger.info("Successfully saved Team: " + savedTeam);

    // Get new team list
    final List<Team> teamsPostUpdate = teamService.fetchAll();
    final int postUpdateTeamCount = teamsPostUpdate.size();
    assertThat(postUpdateTeamCount).isGreaterThan(initialTeamCount);
    assertThat(postUpdateTeamCount - initialTeamCount).isEqualTo(1);

    // Cleanup
    teamService.deleteTeamByName(savingTestTeam.getName().getName());
  }

  @Test
  @DisplayName("Validate team deletion")
  void deleteTeamById() {

    final Team deleteTestTeam = new Team("Delete Test Team");
    // Save to database
    teamService.save(deleteTestTeam);

    // Get team count
    final List<Team> updatedTeams = teamService.fetchAll();
    final int updatedTeamCount = updatedTeams.size();

    // Delete test team
    teamService.deleteTeamByName(deleteTestTeam.getName().getName());
    // Get new team count
    final List<Team> deletedTeamsList = teamService.fetchAll();
    final int deletedTeamsCount = deletedTeamsList.size();
    assertThat(updatedTeamCount).isGreaterThan(deletedTeamsCount);
    assertThat(updatedTeamCount - deletedTeamsCount).isEqualTo(1);
  }
}
