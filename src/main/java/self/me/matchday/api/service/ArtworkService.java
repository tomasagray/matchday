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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import self.me.matchday.db.CompetitionRepository;
import self.me.matchday.db.TeamRepository;
import self.me.matchday.model.Artwork;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Team;

@Service
@PropertySource("classpath:artwork.properties")
public class ArtworkService {

  @Value("${artwork.default-fanart}")
  private String DEFAULT_FANART;

  @Value("${artwork.default-team-emblem}")
  private String DEFAULT_TEAM_EMBLEM;

  @Value("${artwork.default-competition-emblem}")
  private String DEFAULT_COMPETITION_EMBLEM;

  @Value("${artwork.default-competition-landscape}")
  private String DEFAULT_COMPETITION_LANDSCAPE;

  private final TeamRepository teamRepository;
  private final CompetitionRepository competitionRepository;

  @Autowired
  public ArtworkService(
      TeamRepository teamRepository, CompetitionRepository competitionRepository) {

    this.teamRepository = teamRepository;
    this.competitionRepository = competitionRepository;
  }

  /**
   * Retrieves the emblem image for the specified Team. If the emblem has not been set, returns the
   * default team emblem image.
   *
   * @param teamId The name of the Team.
   * @return An optional containing a byte array of the image data.
   */
  public Optional<byte[]> fetchTeamEmblem(@NotNull final UUID teamId) {
    return readTeamArtworkOrDefault(teamId, Team::getEmblem, DEFAULT_TEAM_EMBLEM);
  }

  /**
   * Retrieve the fanart image data for the specified Team.
   *
   * @param teamId The name of the Team.
   * @return A byte array of the image data, or empty() if none found.
   */
  public Optional<byte[]> fetchTeamFanart(@NotNull final UUID teamId) {
    return readTeamArtworkOrDefault(teamId, Team::getFanart, DEFAULT_FANART);
  }

  /**
   * Retrieves the emblem image for the specified Competition. If the emblem has not been set,
   * returns the default artwork.
   *
   * @param competitionId The URL-encoded competitionId of the Competition.
   * @return An Optional containing a byte array of the image data.
   */
  public Optional<byte[]> fetchCompetitionEmblem(@NotNull final UUID competitionId) {
    return readCompetitionArtworkOrDefault(
        competitionId, Competition::getEmblem, DEFAULT_COMPETITION_EMBLEM);
  }

  /**
   * Retrieves the fanart image for the specified Competition. If fanart has not been set, returns
   * the default competition fanart instead.
   *
   * @param competitionId The URL-encoded name of the Competition.
   * @return A byte array of the image data.
   */
  public Optional<byte[]> fetchCompetitionFanart(@NotNull final UUID competitionId) {
    return readCompetitionArtworkOrDefault(
        competitionId, Competition::getFanart, DEFAULT_FANART);
  }

  /**
   * Retrieves the monochrome emblem image data for the specified Competition. If the monochrome
   * emblem has not been set, returns the default instead.
   *
   * @param competitionId The URL-encoded name of the Competition.
   * @return A byte array of the image data.
   */
  public Optional<byte[]> fetchCompetitionMonochromeEmblem(@NotNull final UUID competitionId) {
    return readCompetitionArtworkOrDefault(
        competitionId, Competition::getMonochromeEmblem, DEFAULT_COMPETITION_EMBLEM);
  }

  /**
   * Retrieves the landscape artwork for the given Competition.
   *
   * @param competitionName The URL-encoded name of the Competition.
   * @return A byte array containing the image data.
   */
  public Optional<byte[]> fetchCompetitionLandscape(@NotNull final UUID competitionName) {
    return readCompetitionArtworkOrDefault(
        competitionName, Competition::getLandscape, DEFAULT_COMPETITION_LANDSCAPE);
  }

  private @NotNull File getDefaultFile(@NotNull String resourcePath) {
    try {
      final ClassPathResource resource = new ClassPathResource(resourcePath);
      return resource.getFile().getAbsoluteFile();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @NotNull
  private Optional<byte[]> readTeamArtworkOrDefault(
      @NotNull UUID teamId,
      @NotNull Function<Team, Artwork> getter,
      @NotNull String defaultFilename) {

    final Optional<Team> teamOptional = teamRepository.findById(teamId);
    if (teamOptional.isPresent()) {
      File artwork =
          teamOptional
              .map(getter)
              .map(Artwork::getFile)
              .orElse(getDefaultFile(defaultFilename));
      return Optional.of(readArtworkFromDisk(artwork));
    }
    return Optional.empty();
  }

  @NotNull
  private Optional<byte[]> readCompetitionArtworkOrDefault(
      @NotNull UUID competitionId,
      @NotNull Function<Competition, Artwork> getter,
      @NotNull String defaultFilename) {

    final Optional<Competition> competitionOptional = competitionRepository.findById(competitionId);
    if (competitionOptional.isPresent()) {
      File artwork =
          competitionOptional
              .map(getter)
              .map(Artwork::getFile)
              .orElse(getDefaultFile(defaultFilename));
      return Optional.of(readArtworkFromDisk(artwork));
    }
    return Optional.empty();
  }

  /**
   * Reads artwork from local disk via the classpath (src/main/resources).
   *
   * @param file The filepath of the artwork
   * @return A byte array of the image.
   */
  private byte @NotNull [] readArtworkFromDisk(@NotNull final File file) {
    try (final FileInputStream in = new FileInputStream(file);
        final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      in.transferTo(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
