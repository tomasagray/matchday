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
import self.me.matchday.api.resource.CompetitionResource;
import self.me.matchday.api.resource.EventResource;
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.api.service.ArtworkService;
import self.me.matchday.api.service.CompetitionService;
import self.me.matchday.api.service.EventService;
import self.me.matchday.api.service.TeamService;

@RestController
@RequestMapping(value = "/competitions")
public class CompetitionController {

  private final CompetitionService competitionService;
  private final TeamService teamService;
  private final EventService eventService;
  private final ArtworkService artworkService;

  @Autowired
  public CompetitionController(CompetitionService competitionService, TeamService teamService,
      EventService eventService, ArtworkService artworkService) {

    this.competitionService = competitionService;
    this.teamService = teamService;
    this.eventService = eventService;
    this.artworkService = artworkService;
  }


  /**
   * Provide all Competitions to the API.
   *
   * @return All Competitions as an HttpEntity.
   */
  @RequestMapping(value = "/", method = RequestMethod.GET)
  public ResponseEntity<CollectionModel<CompetitionResource>> fetchAllCompetitions() {
    return
        competitionService
            .fetchAllCompetitions()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Retrieve a single Competition from the local database.
   *
   * @param competitionId MD5 String ID for the desired Competition.
   * @return A Competition Resource.
   */
  @RequestMapping(value = "/competition/{competitionId}", method = RequestMethod.GET)
  public ResponseEntity<CompetitionResource> fetchCompetitionById(
      @PathVariable final String competitionId) {

    return
        competitionService
            .fetchCompetitionById(competitionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Retrieve Teams for a given Competition from the database.
   *
   * @param competitionId The ID of the competition
   * @return A CollectionModel containing the Teams.
   */
  @RequestMapping(value = "/competition/{competitionId}/teams", method = RequestMethod.GET)
  public ResponseEntity<CollectionModel<TeamResource>> fetchCompetitionTeams(
      @PathVariable final String competitionId) {

    return
        teamService
            .fetchTeamsByCompetitionId(competitionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Gets all Events associated with the given Competition from the local database.
   *
   * @param competitionId The ID of the Competition.
   * @return A ResponseEntity containing the CollectionModel of Events.
   */
  @RequestMapping(value = "/competition/{competitionId}/events", method = RequestMethod.GET)
  public ResponseEntity<CollectionModel<EventResource>> fetchCompetitionEvents(
      @PathVariable final String competitionId) {

    return
        eventService
            .fetchEventsForCompetition(competitionId)
            // add a self link to collection
            .map(eventResources ->
                eventResources.add(linkTo(methodOn(CompetitionController.class)
                    .fetchCompetitionEvents(competitionId)).withSelfRel()))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
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
      method = RequestMethod.GET
  )
  public ResponseEntity<byte[]> fetchCompetitionEmblem(@PathVariable final String competitionId) {

    return
        artworkService
            .fetchCompetitionEmblem(competitionId)
            .map(image ->
                ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(image))
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
      method = RequestMethod.GET
  )
  public ResponseEntity<byte[]> fetchCompetitionFanart(@PathVariable final String competitionId) {

    return
        artworkService
            .fetchCompetitionFanart(competitionId)
            .map(image ->
                ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(image))
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
      method = RequestMethod.GET
  )
  public ResponseEntity<byte[]> fetchCompetitionMonochromeEmblem(
      @PathVariable final String competitionId) {

    return
        artworkService
            .fetchCompetitionMonochromeEmblem(competitionId)
            .map(image ->
                ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(image))
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
      method = RequestMethod.GET
  )
  public ResponseEntity<byte[]> fetchCompetitionLandscape(@PathVariable final String competitionId) {

    return
        artworkService
            .fetchCompetitionLandscape(competitionId)
            .map(image ->
                ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(image))
            .orElse(ResponseEntity.notFound().build());
  }
}
