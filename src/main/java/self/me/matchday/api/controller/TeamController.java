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
import self.me.matchday.api.resource.EventResource;
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.api.service.EventService;
import self.me.matchday.api.service.TeamService;

@RestController
public class TeamController {

  private static final String LOG_TAG = "TeamController";

  private final TeamService teamService;
  private final EventService eventService;

  @Autowired
  public TeamController(TeamService teamService, EventService eventService) {

    this.teamService = teamService;
    this.eventService = eventService;
  }

  /**
   * Publish all Teams to the API.
   *
   * @return A List of Teams as an HttpEntity.
   */
  @GetMapping("/teams")
  public ResponseEntity<CollectionModel<TeamResource>> fetchAllTeams() {

    return
        teamService
            .fetchAllTeams()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
  }

  /**
   * Publish a single Team to the API, specified by the Team ID.
   *
   * @param teamId The Team ID (MD5 String)
   * @return The Team as an HttpEntity.
   */
  @GetMapping("/teams/team/{teamId}")
  public ResponseEntity<TeamResource> fetchTeamById(@PathVariable final String teamId) {

    return
        teamService
            .fetchTeamById(teamId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Retrieves all Events associated with the specified Team, and publishes to the API.
   *
   * @param teamId The ID of the Team.
   * @return A CollectionModel of Events.
   */
  @GetMapping("/teams/team/{teamId}/events")
  public ResponseEntity<CollectionModel<EventResource>> fetchEventsForTeam(
      @PathVariable final String teamId) {

    return
        eventService
            .fetchEventsForTeam(teamId)
            // add self link
            .map(eventResources ->
                eventResources.add(linkTo(methodOn(TeamController.class)
                    .fetchEventsForTeam(teamId)).withSelfRel()))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Provides the URL for the emblem image for the specified Team.
   *
   * @param teamId The ID of the Team.
   * @return The URL of the emblem image.
   */
  @GetMapping("/teams/team/{teamId}/emblem")
  public ResponseEntity<URL> fetchTeamEmblemUrl(@PathVariable final String teamId) {

    // TODO: implement Team artwork service

    URL url = null;
    try {
      url = new URL("http://www.team-emblem-url.com");
    } catch (MalformedURLException ignored) {
    }
    return new ResponseEntity<>(url, HttpStatus.OK);
  }
}
