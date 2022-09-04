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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import self.me.matchday.db.MatchRepository;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event.EventSorter;
import self.me.matchday.model.Match;
import self.me.matchday.model.Team;

@Service
@Transactional
public class MatchService implements EntityService<Match, UUID> {

  private static final EventSorter EVENT_SORTER = new EventSorter();

  private final MatchRepository matchRepository;
  private final EntityCorrectionService entityCorrectionService;
  private final TeamService teamService;
  private final CompetitionService competitionService;

  public MatchService(
      MatchRepository matchRepository,
      EntityCorrectionService entityCorrectionService,
      TeamService teamService,
      CompetitionService competitionService) {
    this.matchRepository = matchRepository;
    this.entityCorrectionService = entityCorrectionService;
    this.teamService = teamService;
    this.competitionService = competitionService;
  }

  @Override
  public Match initialize(@NotNull Match match) {
    final Competition competition = match.getCompetition();
    if (competition != null) {
      competitionService.initialize(competition);
    }
    final Team homeTeam = match.getHomeTeam();
    if (homeTeam != null) {
      teamService.initialize(homeTeam);
    }
    final Team awayTeam = match.getAwayTeam();
    if (awayTeam != null) {
      teamService.initialize(awayTeam);
    }
    return match;
  }

  /**
   * Retrieve all Matches from the repo (database) and assemble into a collection of resources.
   *
   * @return Collection of assembled resources.
   */
  @Override
  public List<Match> fetchAll() {

    final List<Match> matches = matchRepository.findAll();
    if (matches.size() > 0) {
      matches.sort(EVENT_SORTER);
      matches.forEach(this::initialize);
    }
    return matches;
  }

  /**
   * Retrieve a specific match from the local DB.
   *
   * @param matchId The ID of the match we want.
   * @return An optional containing the match resource, if it was found.
   */
  @Override
  public Optional<Match> fetchById(@NotNull UUID matchId) {
    return matchRepository.findById(matchId).map(this::initialize);
  }

  /**
   * Retrieve all Events associated with the specified Team.
   *
   * @param teamId The name of the Team.
   * @return A CollectionModel containing the Events.
   */
  public List<Match> fetchMatchesForTeam(@NotNull final UUID teamId) {
    return matchRepository.fetchMatchesByTeam(teamId).stream()
        .map(this::initialize)
        .collect(Collectors.toList());
  }

  @Override
  public Match save(@NotNull Match match) {

    validateMatch(match);
    try {
      entityCorrectionService.correctEntityFields(match);
      // See if Event already exists in DB
      final Optional<Match> eventOptional = matchRepository.findOne(getExampleEvent(match));
      if (eventOptional.isPresent()) {
        final Match existingEvent = eventOptional.get();
        existingEvent.addAllFileSources(match.getFileSources());
        return existingEvent;
      }
      final Match saved = matchRepository.save(match);
      return initialize(saved);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private void validateMatch(@NotNull Match match) {
    teamService.validateTeam(match.getHomeTeam());
    teamService.validateTeam(match.getAwayTeam());
  }

  private @NotNull Example<Match> getExampleEvent(@NotNull Match match) {

    final Match exampleEvent =
        Match.builder()
            .competition(match.getCompetition())
            .season(match.getSeason())
            .fixture(match.getFixture())
            .homeTeam(match.getHomeTeam())
            .awayTeam(match.getAwayTeam())
            .build();
    return Example.of(exampleEvent);
  }

  @Override
  public List<Match> saveAll(@NotNull Iterable<? extends Match> entities) {
    return StreamSupport.stream(entities.spliterator(), false)
        .map(this::save)
        .collect(Collectors.toList());
  }

  @Override
  public Match update(@NotNull Match match) {
    final UUID eventId = match.getEventId();
    final Optional<Match> optional = fetchById(eventId);
    if (optional.isPresent()) {
      return save(match);
    }
    // else..
    throw new IllegalArgumentException("Trying to update non-existent Match with ID: " + eventId);
  }

  @Override
  public List<Match> updateAll(@NotNull Iterable<? extends Match> matches) {
    return StreamSupport.stream(matches.spliterator(), false)
        .map(this::update)
        .collect(Collectors.toList());
  }

  @Override
  public void delete(@NotNull UUID matchId) {
    matchRepository.deleteById(matchId);
  }

  @Override
  public void deleteAll(@NotNull Iterable<? extends Match> matches) {
    matchRepository.deleteAll(matches);
  }
}
