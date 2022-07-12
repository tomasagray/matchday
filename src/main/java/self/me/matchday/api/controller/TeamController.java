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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.EventsResource.EventResourceAssembler;
import self.me.matchday.api.resource.MatchResource;
import self.me.matchday.api.resource.MatchResource.MatchResourceAssembler;
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.api.resource.TeamResource.TeamResourceAssembler;
import self.me.matchday.api.service.EventService;
import self.me.matchday.api.service.MatchService;
import self.me.matchday.api.service.TeamService;
import self.me.matchday.model.Match;

import java.util.List;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/teams")
public class TeamController {

  private final TeamService teamService;
  private final TeamResourceAssembler teamResourceAssembler;
  private final MatchService matchService;
  private final MatchResourceAssembler matchAssembler;

  @Autowired
  public TeamController(
      TeamService teamService,
      TeamResourceAssembler teamResourceAssembler,
      EventService eventService,
      MatchService matchService,
      EventResourceAssembler eventResourceAssembler,
      MatchResourceAssembler matchAssembler) {

    this.teamService = teamService;
    this.teamResourceAssembler = teamResourceAssembler;
    this.matchService = matchService;
    this.matchAssembler = matchAssembler;
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
    return teamResourceAssembler.toCollectionModel(teamService.fetchAll());
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
        .fetchById(teamId)
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
  @RequestMapping(value = "/team/{teamId}/matches", method = RequestMethod.GET)
  public ResponseEntity<CollectionModel<MatchResource>> fetchEventsForTeam(
      @PathVariable final UUID teamId) {

    final List<Match> events = matchService.fetchMatchesForTeam(teamId);
    final CollectionModel<MatchResource> eventResources =
        matchAssembler
            .toCollectionModel(events)
            .add(linkTo(methodOn(TeamController.class).fetchEventsForTeam(teamId)).withSelfRel());
    return ResponseEntity.ok(eventResources);
  }
}
