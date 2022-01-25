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
import self.me.matchday.db.CompetitionRepository;
import self.me.matchday.model.Competition;
import self.me.matchday.util.Log;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class CompetitionService {

  private static final String LOG_TAG = "CompetitionService";

  private final CompetitionRepository competitionRepository;

  @Autowired
  public CompetitionService(final CompetitionRepository competitionRepository) {
    this.competitionRepository = competitionRepository;
  }

  /**
   * Fetch all Competitions in the database.
   *
   * @return A CollectionModel of Competition resources.
   */
  public Optional<List<Competition>> fetchAllCompetitions() {

    Log.i(LOG_TAG, "Retrieving all Competitions from database.");

    final List<Competition> competitions = competitionRepository.findAll();
    if (competitions.size() > 0) {
      // Sort Competitions by name
      competitions.sort(Comparator.comparing(Competition::getName));
      return Optional.of(competitions);
    } else {
      Log.i(LOG_TAG, "Attempted to fetch all Competitions, but none returned");
      return Optional.empty();
    }
  }

  /**
   * Fetch a specific Competition from the database.
   *
   * @param competitionId The ID of the desired Competition.
   * @return The Competition as a resource.
   */
  public Optional<Competition> fetchCompetitionById(@NotNull String competitionId) {

    Log.i(LOG_TAG,
        String.format("Fetching competition with ID: %s from the database.", competitionId));
    return
        competitionRepository
            .findById(competitionId);
  }

  /**
   * Saves the given Competition to the database, if it is valid
   *
   * @param competition The Competition to persist
   * @return The (now Spring-managed) Competition, or null if it was not saved
   */
  @Transactional
  public Competition saveCompetition(@NotNull final Competition competition) {

    if (isValidCompetition(competition)) {
      competitionRepository.saveAndFlush(competition);
      return competition;
    }
    // invalid data...
    return null;
  }

  /**
   * Delete the Competition specified by the given ID from the database
   *
   * @param competitionId The ID of the Competition to delete
   */
  @Transactional
  public void deleteCompetitionById(@NotNull final String competitionId) {

    Log.i(LOG_TAG, String.format("Deleting Competition with ID: [%s] from database", competitionId));
    competitionRepository.deleteById(competitionId);
  }

  /**
   * Data validation for Competition objects
   *
   * @param competition The Competition to scrutinize
   * @return true/false
   */
  private boolean isValidCompetition(@NotNull final Competition competition) {

    return competition.getName() != null;
  }
}
