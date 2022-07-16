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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.CompetitionRepository;
import self.me.matchday.model.Competition;
import self.me.matchday.model.ProperName;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Transactional
public class CompetitionService implements EntityService<Competition> {

  private final CompetitionRepository competitionRepository;

  public CompetitionService(final CompetitionRepository competitionRepository) {
    this.competitionRepository = competitionRepository;
  }

  /**
   * Fetch all Competitions in the database.
   *
   * @return A CollectionModel of Competition resources.
   */
  @Override
  public List<Competition> fetchAll() {

    final List<Competition> competitions = competitionRepository.findAll();
    if (competitions.size() > 0) {
      competitions.sort(Comparator.comparing(Competition::getName));
    }
    return competitions;
  }

  /**
   * Fetch a specific Competition from the database.
   *
   * @param competitionId The ID of the desired Competition.
   * @return The Competition as a resource.
   */
  @Override
  public Optional<Competition> fetchById(@NotNull UUID competitionId) {
    return competitionRepository.findById(competitionId);
  }

  public Optional<Competition> fetchCompetitionByName(@NotNull String name) {
    return competitionRepository.findCompetitionByNameName(name);
  }

  /**
   * Fetch all Competitions from the database in which the given Team
   *
   * @param teamId The ID of the Team
   * @return A list of Competitions
   */
  public List<Competition> fetchCompetitionsForTeam(@NotNull UUID teamId) {
    return competitionRepository.findCompetitionsForTeam(teamId);
  }

  /**
   * Saves the given Competition to the database, if it is valid
   *
   * @param competition The Competition to persist
   * @return The (now Spring-managed) Competition, or null if it was not saved
   */
  @Override
  public Competition save(@NotNull final Competition competition) {

    validateCompetition(competition);
    final Optional<Competition> competitionOptional =
        competitionRepository.findCompetitionByNameName(competition.getName().getName());
    return competitionOptional.orElseGet(() -> competitionRepository.saveAndFlush(competition));
  }

  @Override
  public List<Competition> saveAll(@NotNull Iterable<? extends Competition> competitions) {
    return StreamSupport.stream(competitions.spliterator(), false)
        .map(this::save)
        .collect(Collectors.toList());
  }

  @Override
  public Competition update(@NotNull Competition competition) {
    if (competition.getCompetitionId() == null) {
      throw new IllegalArgumentException("Trying to update unknown Competition: " + competition);
    }
    return save(competition);
  }

  @Override
  public List<Competition> updateAll(@NotNull Iterable<? extends Competition> competitions) {
    return StreamSupport.stream(competitions.spliterator(), false)
        .map(this::update)
        .collect(Collectors.toList());
  }

  /**
   * Delete the Competition specified by the given ID from the database
   *
   * @param competitionId The ID of the Competition to delete
   */
  @Override
  public void delete(@NotNull final UUID competitionId) {
    competitionRepository.deleteById(competitionId);
  }

  @Override
  public void deleteAll(@NotNull Iterable<? extends Competition> competitions) {
    StreamSupport.stream(competitions.spliterator(), false)
        .map(Competition::getCompetitionId)
        .forEach(this::delete);
  }

  /**
   * Data validation for Competition objects
   *
   * @param competition The Competition to scrutinize
   */
  private void validateCompetition(@NotNull final Competition competition) {
    final ProperName name = competition.getName();
    if (name == null || "".equals(name.getName())) {
      throw new IllegalArgumentException("Competition name was blank or null");
    }
  }
}
