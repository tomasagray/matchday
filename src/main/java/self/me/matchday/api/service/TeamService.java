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

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.TeamRepository;
import self.me.matchday.model.*;
import self.me.matchday.model.validation.TeamValidator;

@Service
@Transactional
public class TeamService implements EntityService<Team, UUID> {

  public static final Sort DEFAULT_TEAM_SORT = Sort.by(Direction.ASC, "name.name");

  private final TeamRepository teamRepository;
  private final TeamValidator validator;
  private final ArtworkService artworkService;
  private final SynonymService synonymService;
  private final Map<ArtworkRole, Function<Team, ArtworkCollection>> methodRegistry;

  public TeamService(
      TeamRepository teamRepository,
      TeamValidator validator,
      ArtworkService artworkService,
      SynonymService synonymService) {
    this.teamRepository = teamRepository;
    this.artworkService = artworkService;
    this.synonymService = synonymService;
    this.validator = validator;
    this.methodRegistry = createMethodRegistry();
  }

  private static @NotNull Map<ArtworkRole, Function<Team, ArtworkCollection>>
      createMethodRegistry() {
    final Map<ArtworkRole, Function<Team, ArtworkCollection>> registry = new HashMap<>();
    registry.put(ArtworkRole.EMBLEM, Team::getEmblem);
    registry.put(ArtworkRole.FANART, Team::getFanart);
    return registry;
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
    Hibernate.initialize(team.getColors());
    Hibernate.initialize(team.getEmblem().getCollection());
    Hibernate.initialize(team.getFanart().getCollection());
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

  public Page<Team> fetchAllPaged(final int page, final int size) {
    final PageRequest request = PageRequest.of(page, size, DEFAULT_TEAM_SORT);
    final Page<Team> teams = teamRepository.findAll(request);
    teams.forEach(this::initialize);
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
    validator.validate(team);
    final Team saved = teamRepository.saveAndFlush(team);
    return initialize(saved);
  }

  @Override
  public List<Team> saveAll(@NotNull Iterable<? extends Team> teams) {
    return StreamSupport.stream(teams.spliterator(), false)
        .map(this::save)
        .collect(Collectors.toList());
  }

  @Override
  public Team update(@NotNull Team team) {
    fetchById(team.getId())
        .ifPresentOrElse(
            existing -> validator.validateForUpdate(existing, team),
            () -> {
              throw new IllegalArgumentException("Trying to update non-existent Team: " + team);
            });
    synonymService.updateProperName(team.getName());
    artworkService.repairArtworkFilePaths(team.getEmblem());
    artworkService.repairArtworkFilePaths(team.getFanart());
    return save(team);
  }

  @Override
  public List<Team> updateAll(@NotNull Iterable<? extends Team> teams) {
    return StreamSupport.stream(teams.spliterator(), false)
        .map(this::update)
        .collect(Collectors.toList());
  }

  @Override
  public void delete(@NotNull UUID teamId) throws IOException {

    final Optional<Team> teamOptional = fetchById(teamId);
    if (teamOptional.isPresent()) {
      final Team team = teamOptional.get();
      teamRepository.deleteById(teamId);
      // delete artwork
      final List<Artwork> artworks = new ArrayList<>();
      artworks.addAll(team.getEmblem().getCollection());
      artworks.addAll(team.getFanart().getCollection());
      for (Artwork artwork : artworks) {
        artworkService.deleteArtworkFromDisk(artwork);
      }
    } else {
      throw new IllegalArgumentException("Trying to delete non-existent Team with ID: " + teamId);
    }
  }

  @Override
  public void deleteAll(@NotNull Iterable<? extends Team> teams) throws IOException {
    for (Team team : teams) {
      delete(team.getId());
    }
  }

  /**
   * Delete a Team from the database with the specified ID
   *
   * @param teamName The name of the Team to delete
   */
  public void deleteTeamByName(@NotNull final String teamName) {
    teamRepository.deleteByNameName(teamName);
  }

  public ArtworkCollection fetchArtworkCollection(@NotNull UUID teamId, @NotNull ArtworkRole role) {
    return teamRepository
        .findById(teamId)
        .map(team -> getArtworkCollection(team, role))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format("Could not find %s artwork for Team: %s", role, teamId)));
  }

  private ArtworkCollection getArtworkCollection(@NotNull Team team, @NotNull ArtworkRole role) {
    return methodRegistry.get(role).apply(team);
  }

  public Artwork fetchSelectedArtworkMetadata(@NotNull UUID teamId, @NotNull ArtworkRole role) {
    return fetchArtworkCollection(teamId, role).getSelected();
  }

  public Image fetchSelectedArtwork(@NotNull UUID teamId, @NotNull ArtworkRole role)
      throws IOException {
    final Artwork artwork = fetchSelectedArtworkMetadata(teamId, role);
    if (artwork != null) {
      return artworkService.fetchArtworkData(artwork);
    }
    return null;
  }

  public Artwork fetchArtworkMetadata(
      @NotNull UUID teamId, @NotNull ArtworkRole role, @NotNull Long artworkId) {
    return fetchArtworkCollection(teamId, role).getCollection().stream()
        .filter(artwork -> artworkId.equals(artwork.getId()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "No artwork with ID %s in %s collection for Team: %s",
                        artworkId, role, teamId)));
  }

  public Image fetchArtworkImageData(
      @NotNull UUID teamId, @NotNull ArtworkRole role, @NotNull Long artworkId) throws IOException {
    final Artwork artwork = fetchArtworkMetadata(teamId, role, artworkId);
    if (artwork != null) {
      return artworkService.fetchArtworkData(artwork);
    }
    return null;
  }

  public ArtworkCollection addTeamArtwork(
      @NotNull UUID teamId, @NotNull ArtworkRole role, @NotNull Image image) throws IOException {
    final ArtworkCollection collection = fetchArtworkCollection(teamId, role);
    return artworkService.addArtworkToCollection(collection, image);
  }

  public ArtworkCollection removeTeamArtwork(
      @NotNull UUID teamId, @NotNull ArtworkRole role, @NotNull Long artworkId) throws IOException {
    final ArtworkCollection collection = fetchArtworkCollection(teamId, role);
    return artworkService.deleteArtworkFromCollection(collection, artworkId);
  }
}
