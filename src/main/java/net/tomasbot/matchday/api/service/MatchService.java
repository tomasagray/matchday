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

package net.tomasbot.matchday.api.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.tomasbot.matchday.api.service.video.VideoStreamingService;
import net.tomasbot.matchday.db.MatchRepository;
import net.tomasbot.matchday.model.*;
import net.tomasbot.matchday.model.Event.EventSorter;
import net.tomasbot.matchday.model.validation.EventValidator;
import net.tomasbot.matchday.model.video.VideoFilePack;
import net.tomasbot.matchday.model.video.VideoFileSource;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MatchService implements EntityService<Match, UUID> {

  private static final EventSorter EVENT_SORTER = new EventSorter();

  private final MatchRepository matchRepository;
  private final MatchArtworkService artworkService;
  private final TeamService teamService;
  private final CompetitionService competitionService;
  private final VideoStreamingService streamingService;
  private final EventValidator eventValidator;
  private final EntityCorrectionService entityCorrectionService;

  public MatchService(
      MatchRepository matchRepository,
      MatchArtworkService artworkService,
      TeamService teamService,
      CompetitionService competitionService,
      VideoStreamingService streamingService,
      EventValidator eventValidator,
      EntityCorrectionService entityCorrectionService) {
    this.matchRepository = matchRepository;
    this.artworkService = artworkService;
    this.entityCorrectionService = entityCorrectionService;
    this.teamService = teamService;
    this.competitionService = competitionService;
    this.streamingService = streamingService;
    this.eventValidator = eventValidator;
  }

  private static void nullifyVideoFileIds(@NotNull Match match) {
    match.getFileSources().stream()
        .map(VideoFileSource::getVideoFilePacks)
        .flatMap(List::stream)
        .map(VideoFilePack::allFiles)
        .flatMap((part) -> part.values().stream())
        .forEach(video -> video.setFileId(null));
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
    Hibernate.initialize(match.getArtwork());
    return match;
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
   * Retrieve all Matches from the repo (database) and assemble into a collection of resources.
   *
   * @return Collection of assembled resources.
   */
  @Override
  public List<Match> fetchAll() {
    final List<Match> matches = matchRepository.findAll();
    if (!matches.isEmpty()) {
      matches.sort(EVENT_SORTER);
      matches.forEach(this::initialize);
    }
    return matches;
  }

  public Page<Match> fetchAllPaged(final int page, final int size) {
    final PageRequest request = PageRequest.of(page, size, EventService.DEFAULT_EVENT_SORT);
    final Page<Match> matches = matchRepository.findAll(request);
    matches.forEach(this::initialize);
    return matches;
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

  public Artwork refreshMatchArtwork(@NotNull UUID matchId) throws IOException {
    final Optional<Match> matchOptional = fetchById(matchId);
    if (matchOptional.isPresent()) {
      final Match match = matchOptional.get();
      final Artwork artwork = artworkService.makeMatchArtwork(match);
      match.setArtwork(artwork);
      return artwork;
    }
    // else...
    throw new IllegalArgumentException("Cannot refresh Artwork for non-existent Match: " + matchId);
  }

  public Image fetchMatchArtwork(@NotNull UUID matchId) throws IOException {
    final Optional<Match> optional = fetchById(matchId);
    if (optional.isPresent()) {
      final Match match = optional.get();
      final Artwork artwork = match.getArtwork();
      if (artwork != null) {
        return artworkService.readArtworkData(artwork);
      }
      // else...
      throw new IllegalArgumentException("No artwork for match: " + matchId);
    }
    // else...
    throw new IllegalArgumentException("Cannot fetch Artwork for non-existent Match: " + matchId);
  }

  public Artwork fetchMatchArtworkMetadata(@NotNull UUID matchId) {
    return matchRepository
        .findById(matchId)
        .map(Match::getArtwork)
        .orElseThrow(
            () -> new IllegalArgumentException("No Artwork found for Match ID: " + matchId));
  }

  public Match saveNew(@NotNull Match match) {
    nullifyVideoFileIds(match);
    return save(match);
  }

  @Override
  public Match save(@NotNull Match match) {
    try {
      entityCorrectionService.correctEntityFields(match);
      eventValidator.validate(match);
      // see if Match already exists in DB
      final Optional<Match> eventOptional = matchRepository.findOne(getExampleEvent(match));
      if (eventOptional.isPresent()) {
        Match existing = eventOptional.get();
        existing.addAllFileSources(match.getFileSources());
        return existing;
      }

      final Match entity = matchRepository.saveAndFlush(match);
      // ensure Artwork is attached
      if (entity.getArtwork() == null) {
        final Artwork artwork = artworkService.makeMatchArtwork(entity);
        entity.setArtwork(artwork);
      }
      return initialize(entity);
    } catch (ReflectiveOperationException | IOException e) {
      throw new RuntimeException(e);
    }
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
  public Match update(@NotNull Match updated) {
    final UUID eventId = updated.getEventId();
    final Optional<Match> optional = fetchById(eventId);
    if (optional.isPresent()) {
      final Match existing = optional.get();
      updated.getFileSources().addAll(existing.getFileSources());
      eventValidator.validateForUpdate(existing, updated);
      // perform update...
      updateMatch(existing, updated);
      return save(existing);
    }
    // else..
    throw new IllegalArgumentException("Trying to update non-existent Match with ID: " + eventId);
  }

  private void updateMatch(@NotNull Match existing, @NotNull Match updated) {
    final UUID competitionId = updated.getCompetition().getId();
    final UUID homeTeamId = updated.getHomeTeam().getId();
    final UUID awayTeamId = updated.getAwayTeam().getId();
    competitionService
        .fetchById(competitionId)
        .ifPresentOrElse(
            existing::setCompetition,
            () -> {
              throw new IllegalArgumentException("No competition with ID: " + competitionId);
            });
    teamService
        .fetchById(homeTeamId)
        .ifPresentOrElse(
            existing::setHomeTeam,
            () -> {
              throw new IllegalArgumentException("No Team with ID: " + homeTeamId);
            });
    teamService
        .fetchById(awayTeamId)
        .ifPresentOrElse(
            existing::setAwayTeam,
            () -> {
              throw new IllegalArgumentException("No team with ID: " + awayTeamId);
            });
    existing.setDate(updated.getDate());
    existing.setSeason(updated.getSeason());
    existing.setFixture(updated.getFixture());
  }

  @Override
  public List<Match> updateAll(@NotNull Iterable<? extends Match> matches) {
    return StreamSupport.stream(matches.spliterator(), false)
        .map(this::update)
        .collect(Collectors.toList());
  }

  @Override
  public void delete(@NotNull UUID matchId) throws IOException {
    final Optional<Match> matchOptional = fetchById(matchId);
    if (matchOptional.isPresent()) {
      final Match match = matchOptional.get();
      final Optional<?> existingStream = streamingService.findExistingStream(match);
      if (existingStream.isPresent()) {
        throw new IOException("Cannot delete Match: found existing video streams");
      }
      matchRepository.deleteById(matchId);
      artworkService.deleteArtworkFromDisk(match.getArtwork());
    } else {
      throw new IllegalArgumentException("No Match found with ID: " + matchId);
    }
  }

  @Override
  public void deleteAll(@NotNull Iterable<? extends Match> matches) {
    matchRepository.deleteAll(matches);
  }
}
