/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.api.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.EventResource;
import self.me.matchday.api.resource.EventResource.EventResourceAssembler;
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.api.resource.TeamResource.TeamResourceAssembler;
import self.me.matchday.api.service.ArtworkService;
import self.me.matchday.api.service.EventService;
import self.me.matchday.api.service.TeamService;

@RestController
@RequestMapping(value = "/teams")
public class TeamController {

  private final TeamService teamService;
  private final TeamResourceAssembler teamResourceAssembler;
  private final EventService eventService;
  private final EventResourceAssembler eventResourceAssembler;
  private final ArtworkService artworkService;

  @Autowired
  public TeamController(final TeamService teamService,
      final TeamResourceAssembler teamResourceAssembler,
      final EventService eventService, final EventResourceAssembler eventResourceAssembler,
      final ArtworkService artworkService) {

    this.teamService = teamService;
    this.teamResourceAssembler = teamResourceAssembler;
    this.eventService = eventService;
    this.eventResourceAssembler = eventResourceAssembler;
    this.artworkService = artworkService;
  }

  /**
   * Publish all Teams to the API.
   *
   * @return A List of Teams as an HttpEntity.
   */
  @RequestMapping(value = "/", method = RequestMethod.GET)
  public CollectionModel<TeamResource> fetchAllTeams() {

    return
        teamService
            .fetchAllTeams()
            .map(teamResourceAssembler::toCollectionModel)
            .orElse(null);
  }

  /**
   * Publish a single Team to the API, specified by the Team ID.
   *
   * @param teamId The Team ID (MD5 String)
   * @return The Team as an HttpEntity.
   */
  @RequestMapping(value = "/team/{teamId}", method = RequestMethod.GET)
  public ResponseEntity<TeamResource> fetchTeamById(@PathVariable final String teamId) {

    return
        teamService
            .fetchTeamById(teamId)
            .map(teamResourceAssembler::toModel)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Retrieves all Events associated with the specified Team, and publishes to the API.
   *
   * @param teamId The ID of the Team.
   * @return A CollectionModel of Events.
   */
  @RequestMapping(value = "/team/{teamId}/events", method = RequestMethod.GET)
  public ResponseEntity<CollectionModel<EventResource>> fetchEventsForTeam(
      @PathVariable final String teamId) {

    return
        eventService
            .fetchEventsForTeam(teamId)
            .map(eventResourceAssembler::toCollectionModel)
            // add self link to each EventResource
            .map(eventResources ->
                eventResources
                    .add(linkTo(
                        methodOn(TeamController.class)
                            .fetchEventsForTeam(teamId))
                        .withSelfRel()))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Publishes the Team emblem image to the API.
   *
   * @param teamId The ID of the Team
   * @return A byte array containing the image data; written to response body.
   */
  @RequestMapping(
      value = "/team/{teamId}/emblem",
      produces = MediaType.IMAGE_PNG_VALUE,
      method = RequestMethod.GET
  )
  public ResponseEntity<byte[]> fetchTeamEmblem(@PathVariable final String teamId) {

    return
        artworkService
            .fetchTeamEmblem(teamId)
            .map(image ->
                ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(image))
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
      method = RequestMethod.GET
  )
  public ResponseEntity<byte[]> fetchTeamFanart(@PathVariable final String teamId) {

    return
        artworkService
            .fetchTeamFanart(teamId)
            .map(image ->
                ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(image))
            .orElse(ResponseEntity.notFound().build());
  }
}
