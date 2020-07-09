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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.CompetitionResource;
import self.me.matchday.api.resource.EventResource;
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.api.service.ArtworkService;
import self.me.matchday.api.service.CompetitionService;
import self.me.matchday.api.service.EventService;
import self.me.matchday.api.service.TeamService;

// TODO: Add route prefix annotation
@RestController
public class CompetitionController {

  private static final String LOG_TAG = "CompetitionController";

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
  @GetMapping("/competitions")
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
  @GetMapping("/competitions/competition/{competitionId}")
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
  @GetMapping("/competitions/competition/{competitionId}/teams")
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
  @GetMapping("/competitions/competition/{competitionId}/events")
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
  @GetMapping(
      value = "/competitions/competition/{competitionId}/emblem",
      produces = MediaType.IMAGE_PNG_VALUE
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
  @GetMapping(
      value = "/competitions/competition/{competitionId}/fanart",
      produces = MediaType.IMAGE_JPEG_VALUE
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
  @GetMapping(
      value = "/competitions/competition/{competitionId}/monochrome-emblem",
      produces = MediaType.IMAGE_PNG_VALUE
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
  @GetMapping(
      value = "/competitions/competition/{competitionId}/landscape",
      produces = MediaType.IMAGE_JPEG_VALUE
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
