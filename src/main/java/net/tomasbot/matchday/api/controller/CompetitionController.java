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

package net.tomasbot.matchday.api.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import net.tomasbot.matchday.api.resource.ArtworkCollectionResource;
import net.tomasbot.matchday.api.resource.ArtworkCollectionResource.ArtworkCollectionModeller;
import net.tomasbot.matchday.api.resource.ArtworkResource;
import net.tomasbot.matchday.api.resource.ArtworkResource.ArtworkModeller;
import net.tomasbot.matchday.api.resource.CompetitionResource;
import net.tomasbot.matchday.api.resource.CompetitionResource.CompetitionModeller;
import net.tomasbot.matchday.api.resource.EventsResource;
import net.tomasbot.matchday.api.resource.EventsResource.EventsModeller;
import net.tomasbot.matchday.api.resource.TeamResource;
import net.tomasbot.matchday.api.resource.TeamResource.TeamModeller;
import net.tomasbot.matchday.api.service.CompetitionService;
import net.tomasbot.matchday.api.service.EventService;
import net.tomasbot.matchday.api.service.TeamService;
import net.tomasbot.matchday.model.Artwork;
import net.tomasbot.matchday.model.ArtworkCollection;
import net.tomasbot.matchday.model.ArtworkRole;
import net.tomasbot.matchday.model.Competition;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.Image;
import net.tomasbot.matchday.model.Team;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/competitions")
public class CompetitionController {
  public static final String IMAGE_SVG_VALUE = "image/svg+xml";

  private final EventService eventService;
  private final CompetitionService competitionService;
  private final TeamService teamService;
  private final CompetitionModeller competitionModeller;
  private final TeamModeller teamModeller;
  private final EventsModeller eventsModeller;
  private final ArtworkModeller artworkModeller;
  private final ArtworkCollectionModeller collectionModeller;

  public CompetitionController(
      EventService eventService,
      CompetitionService competitionService,
      TeamService teamService,
      CompetitionModeller competitionModeller,
      TeamModeller teamModeller,
      EventsModeller eventsModeller,
      ArtworkModeller artworkModeller,
      ArtworkCollectionModeller collectionModeller) {

    this.competitionService = competitionService;
    this.competitionModeller = competitionModeller;
    this.teamService = teamService;
    this.teamModeller = teamModeller;
    this.eventService = eventService;
    this.eventsModeller = eventsModeller;
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
  public ResponseEntity<CollectionModel<CompetitionResource>> fetchAllCompetitions() {
    final List<Competition> competitions = competitionService.fetchAll();
    final CollectionModel<CompetitionResource> model =
        competitionModeller.toCollectionModel(competitions);
    model.add(linkTo(methodOn(CompetitionController.class).fetchAllCompetitions()).withSelfRel());
    return ResponseEntity.ok(model);
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
        .map(competitionModeller::toModel)
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
      @PathVariable final UUID competitionId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "16") int size) {

    final Page<Event> eventPage = eventService.fetchEventsForCompetition(competitionId, page, size);
    final EventsResource model = eventsModeller.toModel(eventPage.getContent());
    if (eventPage.hasNext()) {
      model.add(
          linkTo(
                  methodOn(CompetitionController.class)
                      .fetchCompetitionEvents(competitionId, eventPage.getNumber() + 1, size))
              .withRel("next"));
    }
    return ResponseEntity.ok(model);
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
  public ResponseEntity<CollectionModel<TeamResource>> fetchCompetitionTeams(
      @PathVariable final UUID competitionId) {

    final List<Team> teams = teamService.fetchTeamsByCompetitionId(competitionId);
    final CollectionModel<TeamResource> model = teamModeller.toCollectionModel(teams);
    model.add(
        linkTo(methodOn(CompetitionController.class).fetchCompetitionTeams(competitionId))
            .withSelfRel());
    return ResponseEntity.ok(model);
  }

  @RequestMapping(
      value = "/competition/add",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CompetitionResource> addNewCompetition(
      @RequestBody CompetitionResource resource) {
    final Competition competition = competitionModeller.fromModel(resource);
    final Competition saved = competitionService.save(competition);
    final CompetitionResource model = competitionModeller.toModel(saved);
    return ResponseEntity.ok(model);
  }

  @RequestMapping(
      value = "/competition/update",
      method = RequestMethod.PATCH,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CompetitionResource> updateCompetition(
      @RequestBody CompetitionResource resource) {
    final Competition competition = competitionModeller.fromModel(resource);
    final Competition update = competitionService.update(competition);
    final CompetitionResource model = competitionModeller.toModel(update);
    return ResponseEntity.ok(model);
  }

  @RequestMapping(
      value = "/competition/{competitionId}/delete",
      method = RequestMethod.DELETE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UUID> deleteCompetition(@PathVariable UUID competitionId)
      throws IOException {
    competitionService.delete(competitionId);
    return ResponseEntity.ok(competitionId);
  }

  /**
   * Publishes the Competition emblem data to the API.
   *
   * @param competitionId The ID of the Competition.
   * @param role The type of artwork is required
   * @return A byte array of the data.
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
        resource -> CompetitionModeller.addArtworkLinks(competitionId, role, resource));
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
        ? ResponseEntity.ok().contentType(image.contentType()).body(image.data())
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
    return ResponseEntity.ok().contentType(image.contentType()).body(image.data());
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
      @RequestParam("image") MultipartFile data)
      throws IOException {

    final Image image = Image.fromMultipartFile(data);
    final ArtworkCollection collection =
        competitionService.addArtworkToCollection(competitionId, role, image);
    final ArtworkCollectionResource resources = collectionModeller.toModel(collection);
    resources
        .getCollection()
        .forEach(
            artworkResource ->
                CompetitionModeller.addArtworkLinks(competitionId, role, artworkResource));
    resources.add(
        linkTo(
                methodOn(CompetitionController.class)
                    .addCompetitionArtwork(competitionId, role, data))
            .withSelfRel());
    return ResponseEntity.ok(resources);
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
    resource
        .getCollection()
        .forEach(artwork -> CompetitionModeller.addArtworkLinks(competitionId, role, artwork));
    return ResponseEntity.ok(resource);
  }
}
