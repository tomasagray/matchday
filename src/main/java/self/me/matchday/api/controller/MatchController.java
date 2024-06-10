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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static self.me.matchday.api.controller.CompetitionController.IMAGE_SVG_VALUE;

import java.io.IOException;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.ArtworkResource;
import self.me.matchday.api.resource.ArtworkResource.ArtworkModeller;
import self.me.matchday.api.resource.EventsResource;
import self.me.matchday.api.resource.EventsResource.EventsModeller;
import self.me.matchday.api.resource.MatchResource;
import self.me.matchday.api.resource.MatchResource.MatchResourceAssembler;
import self.me.matchday.api.service.InvalidEventException;
import self.me.matchday.api.service.MatchService;
import self.me.matchday.model.Artwork;
import self.me.matchday.model.Image;
import self.me.matchday.model.Match;

@RestController
@RequestMapping(value = "/matches")
public class MatchController {

  private static final Logger logger = LogManager.getLogger(MatchController.class);

  private final MatchService matchService;
  private final EventsModeller eventsAssembler;
  private final MatchResourceAssembler matchAssembler;
  private final ArtworkModeller artworkModeller;

  public MatchController(
      MatchService matchService,
      EventsModeller eventsAssembler,
      MatchResourceAssembler matchAssembler,
      ArtworkModeller artworkModeller) {
    this.matchService = matchService;
    this.eventsAssembler = eventsAssembler;
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
  public ResponseEntity<EventsResource> fetchAllMatches(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "16") int size) {
    final Page<Match> matchPage = matchService.fetchAllPaged(page, size);
    final EventsResource resource = eventsAssembler.toModel(matchPage.getContent());
    if (matchPage.hasNext()) {
      resource.add(
          linkTo(methodOn(MatchController.class).fetchAllMatches(matchPage.getNumber() + 1, size))
              .withRel("next"));
    }
    return ResponseEntity.ok(resource);
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
      value = "/match/add",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MatchResource> addNewMatch(@RequestBody Match match) {
    final Match saved = matchService.saveNew(match);
    final MatchResource model = matchAssembler.toModel(saved);
    return ResponseEntity.ok(model);
  }

  @RequestMapping(
      value = "/match/update",
      method = RequestMethod.PATCH,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MatchResource> updateMatch(@RequestBody final MatchResource resource) {
    final Match match = matchAssembler.fromModel(resource);
    final Match updated = matchService.update(match);
    return ResponseEntity.ok(matchAssembler.toModel(updated));
  }

  @RequestMapping(
      value = "/match/{matchId}/delete",
      method = RequestMethod.DELETE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UUID> deleteMatch(@PathVariable UUID matchId) throws IOException {
    matchService.delete(matchId);
    return ResponseEntity.ok(matchId);
  }

  @RequestMapping(
      value = "/match/{matchId}/artwork",
      method = RequestMethod.GET,
      produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, IMAGE_SVG_VALUE})
  public ResponseEntity<byte[]> fetchMatchArtworkImage(
      @PathVariable UUID matchId, @RequestParam(name = "v", defaultValue = "0") long version)
      throws IOException {
    logger.info("Fetching Artwork for Match: {}, v: {}", matchId, version);
    final Image artwork = matchService.fetchMatchArtwork(matchId);
    return ResponseEntity.ok().contentType(artwork.contentType()).body(artwork.data());
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

  @ExceptionHandler(InvalidEventException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public String handleInvalidEvent(@NotNull InvalidEventException e) {
    String message = e.getMessage();
    logger.error("Found invalid Match: {}", message);
    return message;
  }
}
