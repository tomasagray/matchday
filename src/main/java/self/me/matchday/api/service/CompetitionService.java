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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.CompetitionRepository;
import self.me.matchday.model.Artwork;
import self.me.matchday.model.ArtworkCollection;
import self.me.matchday.model.ArtworkRole;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Country;
import self.me.matchday.model.Image;
import self.me.matchday.model.ProperName;

@Service
@Transactional
public class CompetitionService implements EntityService<Competition, UUID> {

  private final CompetitionRepository competitionRepository;
  private final ArtworkService artworkService;
  private final SynonymService synonymService;
  private final Map<ArtworkRole, Function<Competition, ArtworkCollection>> methodRegistry;

  public CompetitionService(
      CompetitionRepository competitionRepository,
      SynonymService synonymService,
      ArtworkService artworkService) {
    this.competitionRepository = competitionRepository;
    this.synonymService = synonymService;
    this.artworkService = artworkService;
    methodRegistry = createMethodRegistry();
  }

  private static @NotNull Map<ArtworkRole, Function<Competition, ArtworkCollection>>
      createMethodRegistry() {
    final Map<ArtworkRole, Function<Competition, ArtworkCollection>> registry = new HashMap<>();
    registry.put(ArtworkRole.EMBLEM, Competition::getEmblem);
    registry.put(ArtworkRole.FANART, Competition::getFanart);
    return registry;
  }

  @Override
  @Contract("_ -> param1")
  public @NotNull Competition initialize(@NotNull Competition competition) {
    final ProperName name = competition.getName();
    if (name != null) {
      Hibernate.initialize(name.getSynonyms());
    }
    final Country country = competition.getCountry();
    if (country != null) {
      Hibernate.initialize(country.getLocales());
    }

    // initialize Artwork
    Hibernate.initialize(competition.getEmblem().getCollection());
    Hibernate.initialize(competition.getFanart().getCollection());
    return competition;
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
      competitions.forEach(this::initialize);
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
    return competitionRepository.findById(competitionId).map(this::initialize);
  }

  public Optional<Competition> fetchCompetitionByName(@NotNull String name) {
    return competitionRepository.findCompetitionByNameName(name).map(this::initialize);
  }

  /**
   * Fetch all Competitions from the database in which the given Team
   *
   * @param teamId The ID of the Team
   * @return A list of Competitions
   */
  public List<Competition> fetchCompetitionsForTeam(@NotNull UUID teamId) {
    return competitionRepository.findCompetitionsForTeam(teamId).stream()
        .map(this::initialize)
        .collect(Collectors.toList());
  }

  public ArtworkCollection fetchArtworkCollection(
      @NotNull UUID competitionId, @NotNull ArtworkRole role) {
    return fetchById(competitionId)
        .map(competition -> getArtworkCollection(competition, role))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "Could not find %s artwork for Competition: %s", role, competitionId)));
  }

  private ArtworkCollection getArtworkCollection(
      @NotNull Competition competition, @NotNull ArtworkRole role) {
    return methodRegistry.get(role).apply(competition);
  }

  public Artwork fetchSelectedArtworkMetadata(
      @NotNull UUID competitionId, @NotNull ArtworkRole role) {
    return fetchArtworkCollection(competitionId, role).getSelected();
  }

  public Image fetchSelectedArtworkImage(@NotNull UUID competitionId, @NotNull ArtworkRole role)
      throws IOException {
    final Artwork artwork = fetchSelectedArtworkMetadata(competitionId, role);
    if (artwork != null) {
      return artworkService.fetchArtworkData(artwork);
    }
    return null;
  }

  public Artwork fetchArtworkMetadata(
      @NotNull UUID competitionId, @NotNull ArtworkRole role, @NotNull Long artworkId) {
    return fetchArtworkCollection(competitionId, role).getCollection().stream()
        .filter(artwork -> artworkId.equals(artwork.getId()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "No artwork with ID: %s in %s collection for Competition ID: %s",
                        artworkId, role, competitionId)));
  }

  public Image fetchArtworkData(
      @NotNull UUID competitionId, @NotNull ArtworkRole role, @NotNull Long artworkId)
      throws IOException {
    final Artwork artwork = fetchArtworkMetadata(competitionId, role, artworkId);
    return artworkService.fetchArtworkData(artwork);
  }

  public ArtworkCollection addArtworkToCollection(
      @NotNull UUID competitionId, ArtworkRole role, @NotNull Image image) throws IOException {
    final ArtworkCollection collection = fetchArtworkCollection(competitionId, role);
    return artworkService.addArtworkToCollection(collection, image);
  }

  public ArtworkCollection removeCompetitionArtwork(
      @NotNull UUID competitionId, @NotNull ArtworkRole role, @NotNull Long artworkId)
      throws IOException {
    final ArtworkCollection collection = fetchArtworkCollection(competitionId, role);
    return artworkService.deleteArtworkFromCollection(collection, artworkId);
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
    final Competition saved = competitionRepository.saveAndFlush(competition);
    return initialize(saved);
  }

  @Override
  public List<Competition> saveAll(@NotNull Iterable<? extends Competition> competitions) {
    return StreamSupport.stream(competitions.spliterator(), false)
        .map(this::save)
        .collect(Collectors.toList());
  }

  @Override
  public Competition update(@NotNull Competition competition) {
    validateForUpdate(competition);
    // correct missing artwork file paths
    artworkService.repairArtworkFilePaths(competition.getEmblem());
    artworkService.repairArtworkFilePaths(competition.getFanart());
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
  public void delete(@NotNull final UUID competitionId) throws IOException {

    final Optional<Competition> competitionOptional = fetchById(competitionId);
    if (competitionOptional.isPresent()) {
      final Competition competition = competitionOptional.get();
      // save artwork for deletion
      competitionRepository.delete(competition);
      competitionRepository.flush();
      final List<Artwork> artworks = new ArrayList<>();
      artworks.addAll(competition.getEmblem().getCollection());
      artworks.addAll(competition.getFanart().getCollection());
      for (Artwork artwork : artworks) {
        artworkService.deleteArtworkFromDisk(artwork);
      }
    } else {
      throw new IllegalArgumentException(
          "Trying to delete non-existent Competition: " + competitionId);
    }
  }

  @Override
  public void deleteAll(@NotNull Iterable<? extends Competition> competitions) throws IOException {
    for (final Competition competition : competitions) {
      delete(competition.getId());
    }
  }

  private void validateForUpdate(@NotNull Competition updated) {
    validateUpdateId(updated);
    validateUpdateName(updated);
  }

  private void validateUpdateId(@NotNull Competition updated) {

    final UUID updatedId = updated.getId();
    final String unknownMsg = "Trying to update unknown Competition: " + updated;
    if (updatedId == null) {
      throw new UnknownEntityException(unknownMsg);
    }
    final Optional<Competition> optionalIdExists = fetchById(updatedId);
    if (optionalIdExists.isEmpty()) {
      throw new UnknownEntityException(unknownMsg);
    }
  }

  private void validateUpdateName(@NotNull Competition updated) {

    final ProperName updatedProperName = updated.getName();
    synonymService.validateProperName(updatedProperName);

    final String updatedName = updatedProperName.getName();
    final Optional<Competition> optional = fetchCompetitionByName(updatedName);
    if (optional.isPresent()) {
      final Competition existing = optional.get();
      final UUID existingId = existing.getId();
      if (!existingId.equals(updated.getId())) {
        final String msg =
            String.format(
                "A Competition with name: %s already exists; please use the merge function instead",
                updatedName);
        throw new IllegalArgumentException(msg);
      }
    }
  }

  /**
   * Data validation for Competition objects
   *
   * @param competition The Competition to scrutinize
   */
  public void validateCompetition(@NotNull final Competition competition) {
    final ProperName name = competition.getName();
    if (name == null || "".equals(name.getName())) {
      throw new IllegalArgumentException("Competition name was blank or null");
    }
  }
}
