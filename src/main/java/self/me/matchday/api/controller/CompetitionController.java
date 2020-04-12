/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.api.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.net.MalformedURLException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.CompetitionResource;
import self.me.matchday.api.resource.EventResource;
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.api.service.CompetitionService;
import self.me.matchday.api.service.EventService;
import self.me.matchday.api.service.TeamService;
import self.me.matchday.util.Log;

@RestController
public class CompetitionController {

  private static final String LOG_TAG = "CompetitionController";

  private final CompetitionService competitionService;
  private final TeamService teamService;
  private final EventService eventService;

  @Autowired
  public CompetitionController(CompetitionService competitionService, TeamService teamService,
      EventService eventService) {

    this.competitionService = competitionService;
    this.teamService = teamService;
    this.eventService = eventService;
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
   * Get the emblem image for the competition.
   *
   * @param competitionId The ID of the Competition.
   * @return The URL of the competition emblem image.
   */
  @GetMapping("/competitions/competition/{competitionId}/emblem")
  public ResponseEntity<URL> fetchCompetitionEmblemUrl(@PathVariable final String competitionId) {

    Log.i(LOG_TAG, "Getting emblem for competition: " + competitionId);

    // TODO: implement competition emblem service
    URL url = null;
    try {
      url = new URL("http://www.competition-emblem-url.com");
    } catch (MalformedURLException ignored) {
    }

    return new ResponseEntity<>(url, HttpStatus.OK);
  }
}
