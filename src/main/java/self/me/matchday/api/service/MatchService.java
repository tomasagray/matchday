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

import java.awt.Color;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.transaction.Transactional;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import self.me.matchday.db.MatchRepository;
import self.me.matchday.model.Artwork;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event.EventSorter;
import self.me.matchday.model.Image;
import self.me.matchday.model.Match;
import self.me.matchday.model.Param;
import self.me.matchday.model.Team;
import self.me.matchday.util.ResourceFileReader;

@Service
@Transactional
public class MatchService implements EntityService<Match, UUID> {

  private static final EventSorter EVENT_SORTER = new EventSorter();
  private static final Color DEFAULT_HOME_COLOR = new Color(35, 38, 45);
  private static final Color DEFAULT_AWAY_COLOR = new Color(46, 50, 57);

  private final MatchRepository matchRepository;
  private final EntityCorrectionService entityCorrectionService;
  private final TeamService teamService;
  private final CompetitionService competitionService;
  private final ArtworkService artworkService;
  private final byte[] defaultEmblem;

  public MatchService(
      MatchRepository matchRepository,
      EntityCorrectionService entityCorrectionService,
      TeamService teamService,
      CompetitionService competitionService,
      ArtworkService artworkService)
      throws IOException {
    this.matchRepository = matchRepository;
    this.entityCorrectionService = entityCorrectionService;
    this.teamService = teamService;
    this.competitionService = competitionService;
    this.artworkService = artworkService;
    this.defaultEmblem = ResourceFileReader.readBinaryData("image/default_team_emblem.png");
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

  public Artwork makeMatchArtwork(@NotNull Match match) throws IOException {
    final Artwork existingArtwork = match.getArtwork();
    if (existingArtwork != null) {
      final boolean deleted = artworkService.deleteArtwork(existingArtwork);
      if (!deleted) {
        final String msg =
            "Could not create new Match artwork because old artwork could not be deleted";
        throw new IOException(msg);
      }
    }
    final Collection<Param<?>> params = createMatchArtworkParams(match);
    return artworkService.createArtwork(Match.class, params);
  }

  private @NotNull @Unmodifiable Collection<Param<?>> createMatchArtworkParams(@NotNull Match match)
      throws IOException {

    final Team homeTeam = match.getHomeTeam();
    final Team awayTeam = match.getAwayTeam();
    // emblems
    final Param<?> homeTeamEmblem = createTeamEmblemParam(homeTeam, "#home-team-emblem");
    final Param<?> awayTeamEmblem = createTeamEmblemParam(awayTeam, "#away-team-emblem");
    // colors
    final Color[] teamColors = getContrastingTeamColors(homeTeam, awayTeam);
    final Param<Color> homeTeamColor = new Param<>("#home-team-color", teamColors[0]);
    final Param<Color> awayTeamColor = new Param<>("#away-team-color", teamColors[1]);

    return List.of(homeTeamEmblem, awayTeamEmblem, homeTeamColor, awayTeamColor);
  }

  private @NotNull Param<?> createTeamEmblemParam(@NotNull Team team, @NotNull String tag)
      throws IOException {

    final Artwork emblem = team.getEmblem().getSelected();
    byte[] data;
    if (emblem != null) {
      final Image image = artworkService.fetchArtworkData(emblem);
      data = image.getData();
    } else {
      data = Arrays.copyOf(defaultEmblem, defaultEmblem.length);
    }
    return new Param<>(tag, data);
  }

  private Color @NotNull [] getContrastingTeamColors(@NotNull Team home, @NotNull Team away) {

    final List<Color> homeColors = home.getColors();
    final List<Color> awayColors = away.getColors();
    final Color[] colorPair = artworkService.getContrastingColorPair(homeColors, awayColors);
    return colorPair != null ? colorPair : new Color[] {DEFAULT_HOME_COLOR, DEFAULT_AWAY_COLOR};
  }

  public Artwork refreshMatchArtwork(@NotNull UUID matchId) throws IOException {
    final Optional<Match> matchOptional = fetchById(matchId);
    if (matchOptional.isPresent()) {
      final Match match = matchOptional.get();
      final Artwork artwork = makeMatchArtwork(match);
      match.setArtwork(artwork);
      return artwork;
    }
    // else...
    throw new IllegalArgumentException("No Match with ID: " + matchId);
  }

  public Image fetchMatchArtwork(@NotNull UUID matchId) throws IOException {
    final Optional<Match> optional = fetchById(matchId);
    if (optional.isPresent()) {
      final Match match = optional.get();
      final Artwork artwork = match.getArtwork();
      if (artwork != null) {
        return artworkService.fetchArtworkData(artwork);
      }
      // else...
      throw new IllegalArgumentException("No artwork for match: " + matchId);
    }
    // else...
    throw new IllegalArgumentException("No Match with ID: " + matchId);
  }

  public Artwork fetchMatchArtworkMetadata(@NotNull UUID matchId) {
    return matchRepository
        .findById(matchId)
        .map(Match::getArtwork)
        .orElseThrow(
            () -> new IllegalArgumentException("No Artwork found for Match ID: " + matchId));
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
        return initialize(existingEvent);
      }
      // ensure Artwork is attached
      if (match.getArtwork() == null) {
        final Artwork artwork = makeMatchArtwork(match);
        match.setArtwork(artwork);
      }
      final Match saved = matchRepository.saveAndFlush(match);
      return initialize(saved);
    } catch (ReflectiveOperationException | IOException e) {
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
