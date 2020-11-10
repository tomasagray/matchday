/*
 * Copyright (c) 2020.
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import self.me.matchday.db.CompetitionRepository;
import self.me.matchday.db.TeamRepository;
import self.me.matchday.model.Artwork;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Team;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
public class ArtworkService {

  private static final String LOG_TAG = "ArtworkService";

  // Default artwork paths
  private final static String DEFAULT_FANART = "image/fanart/default_fanart.jpg";
  private final static String DEFAULT_TEAM_EMBLEM = "image/emblem/default_team_emblem.png";
  private final static String DEFAULT_COMPETITION_EMBLEM = "image/emblem/default_competition_emblem.png";
  private final static String DEFAULT_COMPETITION_LANDSCAPE = "image/landscape/default_competition_landscape.jpg";

  private final TeamRepository teamRepository;
  private final CompetitionRepository competitionRepository;

  @Autowired
  public ArtworkService(TeamRepository teamRepository, CompetitionRepository competitionRepository) {

    this.teamRepository = teamRepository;
    this.competitionRepository = competitionRepository;
  }

  // Team artwork
  // ===============================================================================================

  /**
   * Retrieves the emblem image for the specified Team. If the emblem has not been set, returns the
   * default team emblem image.
   *
   * @param teamId The ID of the Team.
   * @return An optional containing a byte array of the image data.
   */
  public Optional<byte[]> fetchTeamEmblem(@NotNull final String teamId) {

    Log.i(LOG_TAG, String.format("Fetching emblem for Team: %s", teamId));
    // Get the Team
    final Optional<Team> teamOptional = teamRepository.findById(teamId);
    if (teamOptional.isPresent()) {
      try {
        final Artwork emblem = teamOptional.get().getEmblem();
        if (emblem != null) {
          Log.i(LOG_TAG, String.format("Emblem found for Team: %s", teamId));
          // If emblem set, read & return
          return Optional.of(readArtworkFromDisk(emblem));
        } else {
          Log.i(LOG_TAG, String.format("Emblem not found for Team: %s; returning default", teamId));
          // Return the default emblem instead
          return Optional.of(readDefaultArtworkFromDisk(DEFAULT_TEAM_EMBLEM));
        }
      } catch (IOException | NullPointerException e) {
        Log.e(LOG_TAG, "Could not read emblem image file for Team with ID: " + teamId, e);
      }
    } else {
      Log.e(LOG_TAG, String.format("Team %s not found in database", teamId));
    }
    // Artwork not found
    return Optional.empty();
  }

  /**
   * Retrieve the fanart image data for the specified Team.
   *
   * @param teamId The ID of the Team.
   * @return A byte array of the image data, or empty() if none found.
   */
  public Optional<byte[]> fetchTeamFanart(@NotNull final String teamId) {

    // Get the Team
    final Optional<Team> teamOptional = teamRepository.findById(teamId);
    if (teamOptional.isPresent()) {
      try {
        // Get fanart
        final Artwork fanart = teamOptional.get().getFanart();
        if (fanart != null) {
          // Read image data & return
          return Optional.of(readArtworkFromDisk(fanart));
        } else {
          // return default artwork if none set
          return Optional.of(readDefaultArtworkFromDisk(DEFAULT_FANART));
        }
      } catch (IOException e) {
        Log.e(LOG_TAG, "Could not read fanart data for Team with ID: " + teamId, e);
      }
    }
    // Artwork not found
    return Optional.empty();
  }

  // Competition artwork
  // ===============================================================================================

  /**
   * Retrieves the emblem image for the specified Competition. If the emblem has not been set,
   * returns the default artwork.
   *
   * @param competitionId The ID of the Competition.
   * @return An Optional containing a byte array of the image data.
   */
  public Optional<byte[]> fetchCompetitionEmblem(@NotNull final String competitionId) {

    // Get competition from DB
    final Optional<Competition> competitionOptional = competitionRepository.findById(competitionId);
    if (competitionOptional.isPresent()) {
      try {
        final Artwork emblem = competitionOptional.get().getEmblem();
        if (emblem != null) {
          return Optional.of(readArtworkFromDisk(emblem));
        } else {
          // Return default emblem
          return Optional.of(readDefaultArtworkFromDisk(DEFAULT_COMPETITION_EMBLEM));
        }
      } catch (IOException e) {
        Log.e(LOG_TAG, "Could not read emblem image for Competition with ID: " + competitionId, e);
      }
    }
    // Artwork not found
    return Optional.empty();
  }

  /**
   * Retrieves the fanart image for the specified Competition. If fanart has not been set, returns
   * the default competition fanart instead.
   *
   * @param competitionId The ID of the Competition.
   * @return A byte array of the image data.
   */
  public Optional<byte[]> fetchCompetitionFanart(@NotNull final String competitionId) {

    // Get competition
    final Optional<Competition> competitionOptional = competitionRepository.findById(competitionId);
    if (competitionOptional.isPresent()) {
      try {
        final Artwork fanart = competitionOptional.get().getFanart();
        if (fanart != null) {
          // Read fanart from disk and return
          return Optional.of(readArtworkFromDisk(fanart));
        } else {
          // Read & return default fanart
          return Optional.of(readDefaultArtworkFromDisk(DEFAULT_FANART));
        }
      } catch (IOException e) {
        Log.e(LOG_TAG,
            "Could not read fanart image data from disk for Competition with ID: " + competitionId,
            e);
      }
    }
    // Artwork not found
    return Optional.empty();
  }

  /**
   * Retrieves the monochrome emblem image data for the specified Competition. If the monochrome
   * emblem has not been set, returns the default instead.
   *
   * @param competitionId The ID of the Competition.
   * @return A byte array of the image data.
   */
  public Optional<byte[]> fetchCompetitionMonochromeEmblem(@NotNull final String competitionId) {

    // Get competition
    final Optional<Competition> competitionOptional = competitionRepository.findById(competitionId);
    if (competitionOptional.isPresent()) {
      try {
        final Artwork monochromeEmblem = competitionOptional.get().getMonochromeEmblem();
        if (monochromeEmblem != null) {
          // Read fanart from disk and return
          return Optional.of(readArtworkFromDisk(monochromeEmblem));
        } else {
          // Read & return default fanart
          return Optional.of(readDefaultArtworkFromDisk(DEFAULT_COMPETITION_EMBLEM));
        }
      } catch (IOException e) {
        Log.e(LOG_TAG,
            "Could not read monochrome emblem image data from disk for Competition with ID: "
                + competitionId, e);
      }
    }
    // Artwork not found
    return Optional.empty();
  }

  /**
   * Retrieves the landscape artwork for the given Competition.
   *
   * @param competitionId The ID of the Competition.
   * @return A byte array containing the image data.
   */
  public Optional<byte[]> fetchCompetitionLandscape(@NotNull final String competitionId) {

    // Get competition from DB
    final Optional<Competition> competitionOptional = competitionRepository.findById(competitionId);
    if (competitionOptional.isPresent()) {
      try {
        final Artwork landscape = competitionOptional.get().getLandscape();
        if (landscape != null) {
          // read landscape art from disk & return
          return Optional.of(readArtworkFromDisk(landscape));
        } else {
          // return default artwork if none set
          return Optional.of(readDefaultArtworkFromDisk(DEFAULT_COMPETITION_LANDSCAPE));
        }
      } catch (IOException e) {
        Log.e(LOG_TAG,
            "Could not read landscape image data for Competition with ID: " + competitionId, e);
      }
    }
    // Artwork not found
    return Optional.empty();
  }

  // Helper functions

  /**
   * Reads the given Artwork image file from local disk storage.
   *
   * @param artwork The Artwork object representing the desired image resource.
   * @return A byte array of the image.
   * @throws IOException If the image cannot be read from disk.
   */
  private byte @NotNull [] readArtworkFromDisk(@NotNull final Artwork artwork) throws IOException {

    // Assemble full filepath
    String filepath = artwork.getFilePath() + artwork.getFileName();
    Log.i(LOG_TAG, String.format("Attempting to read %s from disk", filepath));
    // Read image file from disk
    final ClassPathResource resource = new ClassPathResource(filepath);
    final InputStream in = resource.getInputStream();
    byte[] artworkBytes = StreamUtils.copyToByteArray(in);

    Log.i(LOG_TAG, String.format("Read %s bytes", artworkBytes.length));
    return artworkBytes;
  }

  /**
   * Reads default artwork from local disk via the classpath (src/main/resources).
   *
   * @param filepath The relative filepath of the default artwork
   * @return A byte array of the image.
   * @throws IOException If the image could not be read.
   */
  private byte @NotNull [] readDefaultArtworkFromDisk(@NotNull final String filepath)
      throws IOException {

    final ClassPathResource img = new ClassPathResource(filepath);
    return StreamUtils.copyToByteArray(img.getInputStream());
  }
}
