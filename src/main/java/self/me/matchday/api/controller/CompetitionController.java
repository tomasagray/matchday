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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
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
import org.springframework.web.multipart.MultipartFile;
import self.me.matchday.api.resource.ArtworkCollectionResource;
import self.me.matchday.api.resource.ArtworkCollectionResource.ArtworkCollectionResourceAssembler;
import self.me.matchday.api.resource.ArtworkResource;
import self.me.matchday.api.resource.ArtworkResource.ArtworkResourceAssembler;
import self.me.matchday.api.resource.CompetitionResource;
import self.me.matchday.api.resource.CompetitionResource.CompetitionResourceAssembler;
import self.me.matchday.api.resource.EventsResource;
import self.me.matchday.api.resource.EventsResource.EventResourceAssembler;
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.api.resource.TeamResource.TeamResourceAssembler;
import self.me.matchday.api.service.CompetitionService;
import self.me.matchday.api.service.EventService;
import self.me.matchday.api.service.InvalidArtworkException;
import self.me.matchday.api.service.TeamService;
import self.me.matchday.api.service.UnknownEntityException;
import self.me.matchday.model.Artwork;
import self.me.matchday.model.ArtworkCollection;
import self.me.matchday.model.ArtworkRole;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Image;

@RestController
@RequestMapping(value = "/competitions")
public class CompetitionController {
  public static final String IMAGE_SVG_VALUE = "image/svg+xml";

  private final CompetitionService competitionService;
  private final CompetitionResourceAssembler resourceAssembler;
  private final TeamService teamService;
  private final TeamResourceAssembler teamResourceAssembler;
  private final EventService eventService;
  private final EventResourceAssembler eventResourceAssembler;

  private final ArtworkResourceAssembler artworkModeller;
  private final ArtworkCollectionResourceAssembler collectionModeller;

  public CompetitionController(
      CompetitionService competitionService,
      CompetitionResourceAssembler resourceAssembler,
      TeamService teamService,
      TeamResourceAssembler teamResourceAssembler,
      EventService eventService,
      EventResourceAssembler eventResourceAssembler,
      ArtworkResourceAssembler artworkModeller,
      ArtworkCollectionResourceAssembler collectionModeller) {

    this.competitionService = competitionService;
    this.resourceAssembler = resourceAssembler;
    this.teamService = teamService;
    this.teamResourceAssembler = teamResourceAssembler;
    this.eventService = eventService;
    this.eventResourceAssembler = eventResourceAssembler;
    this.artworkModeller = artworkModeller;
    this.collectionModeller = collectionModeller;
  }

  /**
   * Provide all Competitions to the API.
   *
   * @return All Competitions as an HttpEntity.
   */
  @RequestMapping(
      value = {"", "/"},
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CollectionModel<CompetitionResource> fetchAllCompetitions() {
    return resourceAssembler.toCollectionModel(competitionService.fetchAll());
  }

  /**
   * Retrieve a single Competition from the local database.
   *
   * @param competitionId ID for the desired Competition.
   * @return A Competition Resource.
   */
  @RequestMapping(
      value = "/competition/{competitionId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CompetitionResource> fetchCompetitionById(
      @PathVariable final UUID competitionId) {

    return competitionService
        .fetchById(competitionId)
        .map(resourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Gets all Events associated with the given Competition from the local database.
   *
   * @param competitionId The name of the Competition.
   * @return A ResponseEntity containing the CollectionModel of Events.
   */
  @RequestMapping(
      value = "/competition/{competitionId}/events",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EventsResource> fetchCompetitionEvents(
      @PathVariable final UUID competitionId) {

    return ResponseEntity.ok(
        eventResourceAssembler.toModel(eventService.fetchEventsForCompetition(competitionId)));
  }

  /**
   * Retrieve Teams for a given Competition from the database.
   *
   * @param competitionId The name of the competition
   * @return A CollectionModel containing the Teams.
   */
  @RequestMapping(
      value = "/competition/{competitionId}/teams",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CollectionModel<TeamResource> fetchCompetitionTeams(
      @PathVariable final UUID competitionId) {

    return teamResourceAssembler.toCollectionModel(
        teamService.fetchTeamsByCompetitionId(competitionId));
  }

  @RequestMapping(
      value = "/competition/update",
      method = RequestMethod.PATCH,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CompetitionResource> updateCompetition(
      @RequestBody Competition competition) {
    final Competition update = competitionService.update(competition);
    final CompetitionResource resource = resourceAssembler.toModel(update);
    return ResponseEntity.ok(resource);
  }

  /**
   * Publishes the Competition emblem image to the API.
   *
   * @param competitionId The ID of the Competition.
   * @param role The type of artwork is required
   * @return A byte array of the image.
   */
  @RequestMapping(
      value = "/competition/{competitionId}/{role}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.GET)
  public ResponseEntity<CollectionModel<ArtworkResource>> fetchCompetitionArtworkCollection(
      @PathVariable final UUID competitionId, @PathVariable ArtworkRole role) {

    final ArtworkCollection collection =
        competitionService.fetchArtworkCollection(competitionId, role);
    final CollectionModel<ArtworkResource> resources = artworkModeller.fromCollection(collection);
    resources.add(
        linkTo(
                methodOn(CompetitionController.class)
                    .fetchCompetitionArtworkCollection(competitionId, role))
            .withSelfRel());
    resources.forEach(
        resource -> CompetitionResourceAssembler.addArtworkLinks(competitionId, role, resource));
    return ResponseEntity.ok(resources);
  }

  @RequestMapping(
      value = "/competition/{competitionId}/{role}/selected",
      method = RequestMethod.GET,
      produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, IMAGE_SVG_VALUE})
  public ResponseEntity<byte[]> fetchSelectedArtwork(
      @PathVariable UUID competitionId, @PathVariable ArtworkRole role) throws IOException {

    final Image image = competitionService.fetchSelectedArtworkImage(competitionId, role);
    return image != null
        ? ResponseEntity.ok().contentType(image.getContentType()).body(image.getData())
        : ResponseEntity.notFound().build();
  }

  @RequestMapping(
      value = "/competition/{competitionId}/{role}/selected/metadata",
      produces = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.GET)
  public ResponseEntity<ArtworkResource> fetchSelectedArtworkMetadata(
      @PathVariable UUID competitionId, @PathVariable ArtworkRole role) throws IOException {

    final Artwork artwork = competitionService.fetchSelectedArtworkMetadata(competitionId, role);
    if (artwork != null) {
      final ArtworkResource model = artworkModeller.toModel(artwork);
      model.add(
          linkTo(methodOn(CompetitionController.class).fetchSelectedArtwork(competitionId, role))
              .withSelfRel());
      model.add(
          linkTo(
                  methodOn(CompetitionController.class)
                      .fetchSelectedArtworkMetadata(competitionId, role))
              .withRel("data"));
      return ResponseEntity.ok(model);
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @RequestMapping(
      value = "/competition/{competitionId}/{role}/{artworkId}",
      method = RequestMethod.GET,
      produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, IMAGE_SVG_VALUE})
  @ResponseBody
  public ResponseEntity<byte[]> fetchArtworkImageData(
      @PathVariable UUID competitionId,
      @PathVariable ArtworkRole role,
      @PathVariable Long artworkId)
      throws IOException {

    final Image image = competitionService.fetchArtworkData(competitionId, role, artworkId);
    return ResponseEntity.ok().contentType(image.getContentType()).body(image.getData());
  }

  @RequestMapping(
      value = "/competition/{competitionId}/{role}/{artworkId}/metadata",
      produces = MediaType.APPLICATION_JSON_VALUE,
      method = RequestMethod.GET)
  public ResponseEntity<ArtworkResource> fetchArtworkMetadata(
      @PathVariable UUID competitionId,
      @PathVariable ArtworkRole role,
      @PathVariable Long artworkId)
      throws IOException {

    final Artwork artwork = competitionService.fetchArtworkMetadata(competitionId, role, artworkId);
    final ArtworkResource model = artworkModeller.toModel(artwork);
    model.add(
        linkTo(
                methodOn(CompetitionController.class)
                    .fetchArtworkMetadata(competitionId, role, artworkId))
            .withSelfRel());
    model.add(
        linkTo(
                methodOn(CompetitionController.class)
                    .fetchArtworkImageData(competitionId, role, artworkId))
            .withSelfRel());
    return ResponseEntity.ok(model);
  }

  @RequestMapping(
      value = "/competition/{competitionId}/{role}/add",
      method = RequestMethod.POST,
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ArtworkCollectionResource> addCompetitionArtwork(
      @PathVariable UUID competitionId,
      @PathVariable ArtworkRole role,
      @RequestParam("image") MultipartFile image)
      throws IOException {

    final ArtworkCollection collection =
        competitionService.addArtworkToCollection(competitionId, role, image);
    final ArtworkCollectionResource resources = collectionModeller.toModel(collection);
    resources
        .getArtwork()
        .forEach(
            artworkResource ->
                CompetitionResourceAssembler.addArtworkLinks(competitionId, role, artworkResource));
    resources.add(
        linkTo(
                methodOn(CompetitionController.class)
                    .addCompetitionArtwork(competitionId, role, image))
            .withSelfRel());
    return ResponseEntity.ok(resources);
  }

  private static void addArtworkLinks(
      @NotNull ArtworkResource artwork, @NotNull UUID competitionId, @NotNull ArtworkRole role) {

    try {
      final Long artworkId = artwork.getId();
      artwork.add(
          linkTo(
                  methodOn(CompetitionController.class)
                      .fetchArtworkImageData(competitionId, role, artworkId))
              .withRel("image"));
      artwork.add(
          linkTo(
                  methodOn(CompetitionController.class)
                      .fetchArtworkMetadata(competitionId, role, artworkId))
              .withRel("metadata"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @RequestMapping(
      value = "/competition/{competitionId}/{role}/{artworkId}/remove",
      method = RequestMethod.DELETE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ArtworkCollectionResource> removeCompetitionArtwork(
      @PathVariable UUID competitionId,
      @PathVariable ArtworkRole role,
      @PathVariable Long artworkId)
      throws IOException {
    final ArtworkCollection collection =
        competitionService.removeCompetitionArtwork(competitionId, role, artworkId);
    final ArtworkCollectionResource resource = collectionModeller.toModel(collection);
    // add links
    resource.getArtwork().forEach(artwork -> addArtworkLinks(artwork, competitionId, role));
    return ResponseEntity.ok(resource);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public String handleIllegalArg(@NotNull IllegalArgumentException e) {
    return e.getMessage();
  }

  @ExceptionHandler(InvalidArtworkException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public String handleInvalidArt(@NotNull InvalidArtworkException e) {
    return e.getMessage();
  }

  @ExceptionHandler(UnknownEntityException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public String handleUnknownEntity(@NotNull UnknownEntityException e) {
    return e.getMessage();
  }

  @ExceptionHandler(FileNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public String handleFileNotFound(@NotNull FileNotFoundException e) {
    return "File not found: " + e.getMessage();
  }

  @ExceptionHandler(IOException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public String handleIoError(@NotNull IOException e) {
    return e.getMessage();
  }
}
