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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.TeamRepository;
import self.me.matchday.model.Country;
import self.me.matchday.model.ProperName;
import self.me.matchday.model.Team;

@Service
@Transactional
public class TeamService implements EntityService<Team, UUID> {

  private final TeamRepository teamRepository;

  @Autowired
  public TeamService(@NotNull final TeamRepository teamRepository) {
    this.teamRepository = teamRepository;
  }

  @Override
  public Team initialize(@NotNull Team team) {
    final Country country = team.getCountry();
    if (country != null) {
      Hibernate.initialize(country.getLocales());
    }
    final ProperName name = team.getName();
    if (name != null) {
      Hibernate.initialize(name.getSynonyms());
    }
    return team;
  }

  /**
   * Fetch all teams from the local database.
   *
   * @return A collection of Teams.
   */
  @Override
  public List<Team> fetchAll() {
    final List<Team> teams = teamRepository.findAll();
    if (teams.size() > 0) {
      teams.sort(Comparator.comparing(Team::getName));
      teams.forEach(this::initialize);
    }
    return teams;
  }

  /**
   * Fetch a single Team from the database, given an ID.
   *
   * @param teamId The Team ID.
   * @return The requested Team, wrapped in an Optional.
   */
  @Override
  public Optional<Team> fetchById(@NotNull final UUID teamId) {
    return teamRepository.findById(teamId).map(this::initialize);
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
    List<Team> teams = new ArrayList<>(teamSet);
    teams.sort(Comparator.comparing(Team::getName));
    teams.forEach(this::initialize);
    return teams;
  }

  public Optional<Team> getTeamByName(@NotNull String name) {
    return teamRepository.findTeamByNameName(name).map(this::initialize);
  }

  /**
   * Saves the given Team to the database, if it is valid
   *
   * @param team The Team to persist
   * @return The (now Spring-managed) Team, or null if invalid data was passed
   */
  @Override
  public Team save(@NotNull final Team team) {
    validateTeam(team);
    final Optional<Team> teamOptional = teamRepository.findTeamByNameName(team.getName().getName());
    final Team saved = teamOptional.orElseGet(() -> teamRepository.saveAndFlush(team));
    initialize(saved);
    return saved;
  }

  @Override
  public List<Team> saveAll(@NotNull Iterable<? extends Team> teams) {
    return StreamSupport.stream(teams.spliterator(), false)
        .map(this::save)
        .collect(Collectors.toList());
  }

  @Override
  public Team update(@NotNull Team team) {
    if (team.getTeamId() == null) {
      throw new IllegalArgumentException("Trying to update unknown Team: " + team);
    }
    return save(team);
  }

  @Override
  public List<Team> updateAll(@NotNull Iterable<? extends Team> teams) {
    return StreamSupport.stream(teams.spliterator(), false)
        .map(this::update)
        .collect(Collectors.toList());
  }

  @Override
  public void delete(@NotNull UUID teamId) {
    teamRepository.deleteById(teamId);
  }

  @Override
  public void deleteAll(@NotNull Iterable<? extends Team> teams) {
    StreamSupport.stream(teams.spliterator(), false).map(Team::getTeamId).forEach(this::delete);
  }

  /**
   * Delete a Team from the database with the specified ID
   *
   * @param teamName The name of the Team to delete
   */
  public void deleteTeamByName(@NotNull final String teamName) {
    teamRepository.deleteByNameName(teamName);
  }

  /**
   * Team data validation
   *
   * @param team The Team to validate
   */
  public void validateTeam(Team team) {
    if (team == null) {
      throw new IllegalArgumentException("Team was null");
    }
    final ProperName properName = team.getName();
    if (properName == null || properName.getName() == null || properName.getName().equals("")) {
      throw new IllegalArgumentException("Team has invalid name");
    }
  }
}
