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

import static self.me.matchday.api.controller.CompetitionController.IMAGE_SVG_VALUE;

import java.io.IOException;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.ArtworkResource;
import self.me.matchday.api.resource.ArtworkResource.ArtworkResourceAssembler;
import self.me.matchday.api.resource.EventsResource;
import self.me.matchday.api.resource.EventsResource.EventResourceAssembler;
import self.me.matchday.api.resource.MatchResource;
import self.me.matchday.api.resource.MatchResource.MatchResourceAssembler;
import self.me.matchday.api.service.MatchService;
import self.me.matchday.model.Artwork;
import self.me.matchday.model.Image;

@RestController
@RequestMapping(value = "/matches")
public class MatchController {

  private final MatchService matchService;
  private final EventResourceAssembler eventAssembler;
  private final MatchResourceAssembler matchAssembler;
  private final ArtworkResourceAssembler artworkModeller;

  @Autowired
  public MatchController(
      MatchService matchService,
      EventResourceAssembler eventAssembler,
      MatchResourceAssembler matchAssembler,
      ArtworkResourceAssembler artworkModeller) {

    this.matchService = matchService;
    this.eventAssembler = eventAssembler;
    this.matchAssembler = matchAssembler;
    this.artworkModeller = artworkModeller;
  }

  /**
   * Fetch all Matches from local DB and return as a response entity.
   *
   * @return A List of Matches as an HttpEntity.
   */
  @RequestMapping(
      value = {"", "/"},
      method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<EventsResource> fetchAllMatches() {
    return ResponseEntity.ok(eventAssembler.toModel(matchService.fetchAll()));
  }

  /**
   * Fetch a specific Match from the local DB, specified by the Match ID
   *
   * @param matchId Identifier for the Match
   * @return A Match as an HttpEntity
   */
  @RequestMapping(value = "/match/{matchId}", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<MatchResource> fetchMatchById(@PathVariable UUID matchId) {

    return matchService
        .fetchById(matchId)
        .map(matchAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(
      value = "/match/{matchId}/artwork",
      method = RequestMethod.GET,
      produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, IMAGE_SVG_VALUE})
  public ResponseEntity<byte[]> fetchMatchArtworkImage(@PathVariable UUID matchId)
      throws IOException {

    final Image artwork = matchService.fetchMatchArtwork(matchId);
    return ResponseEntity.ok().contentType(artwork.getContentType()).body(artwork.getData());
  }

  @RequestMapping(
      value = "/match/{matchId}/artwork/metadata",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ArtworkResource> fetchMatchArtworkMetadata(@PathVariable UUID matchId) {
    final Artwork artwork = matchService.fetchMatchArtworkMetadata(matchId);
    return ResponseEntity.ok().body(artworkModeller.toModel(artwork));
  }

  @RequestMapping(
      value = "/match/{matchId}/artwork/refresh",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ArtworkResource> refreshMatchArtwork(@PathVariable UUID matchId)
      throws IOException {
    final Artwork artwork = matchService.refreshMatchArtwork(matchId);
    return ResponseEntity.ok().body(artworkModeller.toModel(artwork));
  }

  @ExceptionHandler(IOException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public String handleIoError(@NotNull IOException e) {
    return e.getMessage();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public String handleIllegalArg(@NotNull IllegalArgumentException e) {
    return e.getMessage();
  }
}
