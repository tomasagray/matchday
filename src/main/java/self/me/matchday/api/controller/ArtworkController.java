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

package self.me.matchday.api.controller;

import java.io.IOException;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.service.ArtworkService;

@RestController
@RequestMapping(value = "/artwork")
public class ArtworkController {

  private static final Logger logger = LogManager.getLogger(ArtworkService.class);
  private final ArtworkService artworkService;

  @Autowired
  public ArtworkController(final ArtworkService artworkService) {
    this.artworkService = artworkService;
  }

  /**
   * Publishes the Competition emblem image to the API.
   *
   * @param competitionId The ID of the Competition.
   * @return A byte array of the image.
   */
  @RequestMapping(
      value = "/competition/{competitionId}/emblem",
      produces = MediaType.IMAGE_PNG_VALUE,
      method = RequestMethod.GET)
  public ResponseEntity<byte[]> fetchCompetitionEmblem(@PathVariable final UUID competitionId) {

    return artworkService
        .fetchCompetitionEmblem(competitionId)
        .map(image -> ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Publishes the Competition fanart image to the API.
   *
   * @param competitionId The ID of the Competition
   * @return A byte array of the image data.
   */
  @RequestMapping(
      value = "/competition/{competitionId}/fanart",
      produces = MediaType.IMAGE_JPEG_VALUE,
      method = RequestMethod.GET)
  public ResponseEntity<byte[]> fetchCompetitionFanart(@PathVariable final UUID competitionId) {

    return artworkService
        .fetchCompetitionFanart(competitionId)
        .map(image -> ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Publishes the monochrome emblem for the Competition to the API.
   *
   * @param competitionId The ID of the Competition.
   * @return A byte array containing the image data.
   */
  @RequestMapping(
      value = "/competition/{competitionId}/monochrome-emblem",
      produces = MediaType.IMAGE_PNG_VALUE,
      method = RequestMethod.GET)
  public ResponseEntity<byte[]> fetchCompetitionMonochromeEmblem(
      @PathVariable final UUID competitionId) {

    return artworkService
        .fetchCompetitionMonochromeEmblem(competitionId)
        .map(image -> ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Publishes landscape image for the Competition to the API.
   *
   * @param competitionId The ID of the Competition.
   * @return A byte array containing the image data.
   */
  @RequestMapping(
      value = "/competition/{competitionId}/landscape",
      produces = MediaType.IMAGE_JPEG_VALUE,
      method = RequestMethod.GET)
  public ResponseEntity<byte[]> fetchCompetitionLandscape(@PathVariable final UUID competitionId) {

    return artworkService
        .fetchCompetitionLandscape(competitionId)
        .map(image -> ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Publishes the Team emblem image to the API.
   *
   * @param teamId The name of the Team
   * @return A byte array containing the image data; written to response body.
   */
  @RequestMapping(
      value = "/team/{teamId}/emblem",
      produces = MediaType.IMAGE_PNG_VALUE,
      method = RequestMethod.GET)
  public ResponseEntity<byte[]> fetchTeamEmblem(@PathVariable final UUID teamId) {

    return artworkService
        .fetchTeamEmblem(teamId)
        .map(image -> ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Publishes the fanart for the Team to the API, if available.
   *
   * @param teamId The ID of the Team.
   * @return A byte array of the image data.
   */
  @RequestMapping(
      value = "/team/{teamId}/fanart",
      produces = MediaType.IMAGE_JPEG_VALUE,
      method = RequestMethod.GET)
  public ResponseEntity<byte[]> fetchTeamFanart(@PathVariable final UUID teamId) {

    return artworkService
        .fetchTeamFanart(teamId)
        .map(image -> ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image))
        .orElse(ResponseEntity.notFound().build());
  }

  @ExceptionHandler(IOException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<String> handleIoException(@NotNull IOException e) {
    String message = e.getMessage();
    final Throwable cause = e.getCause();
    logger.error("Error reading data from disk: {}; caused by: {}", message, cause.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
  }
}
