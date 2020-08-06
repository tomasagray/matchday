/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.api.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.EventResource;
import self.me.matchday.api.resource.EventResource.EventResourceAssembler;
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.api.resource.TeamResource.TeamResourceAssembler;
import self.me.matchday.api.service.EventService;
import self.me.matchday.api.service.TeamService;

@RestController
@RequestMapping(value = "/teams")
public class TeamController {

  private final TeamService teamService;
  private final TeamResourceAssembler teamResourceAssembler;
  private final EventService eventService;
  private final EventResourceAssembler eventResourceAssembler;

  @Autowired
  public TeamController(final TeamService teamService,
      final TeamResourceAssembler teamResourceAssembler,
      final EventService eventService, final EventResourceAssembler eventResourceAssembler) {

    this.teamService = teamService;
    this.teamResourceAssembler = teamResourceAssembler;
    this.eventService = eventService;
    this.eventResourceAssembler = eventResourceAssembler;
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
}
