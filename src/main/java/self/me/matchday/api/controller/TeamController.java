/*
 * Copyright (c) 2020.
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

import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/teams")
public class TeamController {

  private final TeamService teamService;
  private final TeamResourceAssembler teamResourceAssembler;
  private final EventService eventService;
  private final EventResourceAssembler eventResourceAssembler;

  @Autowired
  public TeamController(
      final TeamService teamService,
      final TeamResourceAssembler teamResourceAssembler,
      final EventService eventService,
      final EventResourceAssembler eventResourceAssembler) {

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
  @RequestMapping(
      value = {"", "/"},
      method = RequestMethod.GET)
  public CollectionModel<TeamResource> fetchAllTeams() {

    return teamService.fetchAllTeams().map(teamResourceAssembler::toCollectionModel).orElse(null);
  }

  /**
   * Publish a single Team to the API, specified by the Team name.
   *
   * @param teamId The Team name (MD5 String)
   * @return The Team as an HttpEntity.
   */
  @RequestMapping(value = "/team/{teamId}", method = RequestMethod.GET)
  public ResponseEntity<TeamResource> fetchTeamByName(@PathVariable final UUID teamId) {

    return teamService
        .fetchTeamById(teamId)
        .map(teamResourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Retrieves all Events associated with the specified Team, and publishes to the API.
   *
   * @param teamId The name of the Team.
   * @return A CollectionModel of Events.
   */
  @RequestMapping(value = "/team/{teamId}/events", method = RequestMethod.GET)
  public ResponseEntity<CollectionModel<EventResource>> fetchEventsForTeam(
      @PathVariable final UUID teamId) {

    return eventService
        .fetchEventsForTeam(teamId)
        .map(eventResourceAssembler::toCollectionModel)
        // add self link to each EventResource
        .map(
            eventResources ->
                eventResources.add(
                    linkTo(methodOn(TeamController.class).fetchEventsForTeam(teamId))
                        .withSelfRel()))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
