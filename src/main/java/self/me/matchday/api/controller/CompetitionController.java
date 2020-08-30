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
import self.me.matchday.api.resource.CompetitionResource;
import self.me.matchday.api.resource.CompetitionResource.CompetitionResourceAssembler;
import self.me.matchday.api.resource.EventResource;
import self.me.matchday.api.resource.EventResource.EventResourceAssembler;
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.api.resource.TeamResource.TeamResourceAssembler;
import self.me.matchday.api.service.CompetitionService;
import self.me.matchday.api.service.EventService;
import self.me.matchday.api.service.TeamService;

@RestController
@RequestMapping(value = "/competitions")
public class CompetitionController {

  private final CompetitionService competitionService;
  private final CompetitionResourceAssembler competitionResourceAssembler;
  private final TeamService teamService;
  private final TeamResourceAssembler teamResourceAssembler;
  private final EventService eventService;
  private final EventResourceAssembler eventResourceAssembler;

  @Autowired
  public CompetitionController(final CompetitionService competitionService,
      final CompetitionResourceAssembler competitionResourceAssembler,
      final TeamService teamService, final TeamResourceAssembler teamResourceAssembler,
      final EventService eventService, final EventResourceAssembler eventResourceAssembler) {

    this.competitionService = competitionService;
    this.competitionResourceAssembler = competitionResourceAssembler;
    this.teamService = teamService;
    this.teamResourceAssembler = teamResourceAssembler;
    this.eventService = eventService;
    this.eventResourceAssembler = eventResourceAssembler;
  }


  /**
   * Provide all Competitions to the API.
   *
   * @return All Competitions as an HttpEntity.
   */
  @RequestMapping(value = "/", method = RequestMethod.GET)
  public CollectionModel<CompetitionResource> fetchAllCompetitions() {
    return
        competitionService
            .fetchAllCompetitions()
            .map(competitionResourceAssembler::toCollectionModel)
            .orElse(null);
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
            .map(competitionResourceAssembler::toModel)
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
  public CollectionModel<TeamResource> fetchCompetitionTeams(
      @PathVariable final String competitionId) {

    return
        teamService
            .fetchTeamsByCompetitionId(competitionId)
            .map(teamResourceAssembler::toCollectionModel)
            .orElse(null);
  }

  /**
   * Gets all Events associated with the given Competition from the local database.
   *
   * @param competitionId The ID of the Competition.
   * @return A ResponseEntity containing the CollectionModel of Events.
   */
  @RequestMapping(value = "/competition/{competitionId}/events", method = RequestMethod.GET)
  public CollectionModel<EventResource> fetchCompetitionEvents(@PathVariable final String competitionId) {

    return
        eventService
            .fetchEventsForCompetition(competitionId)
            .map(eventResourceAssembler::toCollectionModel)
            .orElse(null);
  }
}
