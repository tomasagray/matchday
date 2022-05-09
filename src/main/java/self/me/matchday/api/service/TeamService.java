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

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.TeamRepository;
import self.me.matchday.model.Team;
import self.me.matchday.util.Log;

import java.util.*;

@Service
@Transactional
public class TeamService {

  private static final String LOG_TAG = "TeamService";

  private final TeamRepository teamRepository;

  @Autowired
  public TeamService(@NotNull final TeamRepository teamRepository) {
    this.teamRepository = teamRepository;
  }

  /**
   * Fetch all teams from the local database.
   *
   * @return A collection of Teams.
   */
  public List<Team> fetchAllTeams() {

    Log.i(LOG_TAG, "Fetching all Teams from local database.");

    final List<Team> teams = teamRepository.findAll();
    if (teams.size() > 0) {
      // Sort Teams by name
      teams.sort(Comparator.comparing(Team::getProperName));
    }
    return teams;
  }

  /**
   * Fetch a single Team from the database, given an ID.
   *
   * @param teamId The Team ID.
   * @return The requested Team, wrapped in an Optional.
   */
  public Optional<Team> fetchTeamById(@NotNull final UUID teamId) {
    return teamRepository.findById(teamId);
  }

  /**
   * Retrieve all Teams for a given Competition, specified by the competitionId.
   *
   * @param competitionId The ID of the Competition.
   * @return All Teams which have Events in the given Competition.
   */
  public List<Team> fetchTeamsByCompetitionId(@NotNull final UUID competitionId) {

    final List<Team> homeTeams = teamRepository.fetchHomeTeamsByCompetition(competitionId);
    final List<Team> awayTeams = teamRepository.fetchAwayTeamsByCompetition(competitionId);

    // Combine results in a Set<> to ensure no duplicates
    Set<Team> teamSet = new LinkedHashSet<>(homeTeams);
    teamSet.addAll(awayTeams);
    List<Team> teamList = new ArrayList<>(teamSet);
    teamList.sort(Comparator.comparing(Team::getProperName));
    return teamList;
  }

  public Optional<Team> getTeamByName(@NotNull String name) {
    return teamRepository.findTeamByProperNameName(name);
  }

  /**
   * Saves the given Team to the database, if it is valid
   *
   * @param team The Team to persist
   * @return The (now Spring-managed) Team, or null if invalid data was passed
   */
  public Team saveTeam(@NotNull final Team team) {
    if (isValidTeam(team)) {
      teamRepository.saveAndFlush(team);
      return team;
    }
    // invalid data...
    return null;
  }

  /**
   * Delete a Team from the database with the specified ID
   *
   * @param teamName The name of the Team to delete
   */
  public void deleteTeamByName(@NotNull final String teamName) {

    Log.i(LOG_TAG, String.format("Deleting Team with ID: %s from database", teamName));
    teamRepository.deleteByProperNameName(teamName);
  }

  /**
   * Team data validation
   *
   * @param team The Team to validate
   * @return true/false
   */
  private boolean isValidTeam(@NotNull final Team team) {
    return team.getProperName() != null;
  }
}
