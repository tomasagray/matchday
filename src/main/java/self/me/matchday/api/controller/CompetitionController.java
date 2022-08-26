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

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.CompetitionResource;
import self.me.matchday.api.resource.CompetitionResource.CompetitionResourceAssembler;
import self.me.matchday.api.resource.EventsResource;
import self.me.matchday.api.resource.EventsResource.EventResourceAssembler;
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.api.resource.TeamResource.TeamResourceAssembler;
import self.me.matchday.api.service.CompetitionService;
import self.me.matchday.api.service.EventService;
import self.me.matchday.api.service.TeamService;
import self.me.matchday.api.service.UnknownEntityException;
import self.me.matchday.model.Competition;

@RestController
@RequestMapping(value = "/competitions")
public class CompetitionController {

  private final CompetitionService competitionService;
  private final CompetitionResourceAssembler resourceAssembler;
  private final TeamService teamService;
  private final TeamResourceAssembler teamResourceAssembler;
  private final EventService eventService;
  private final EventResourceAssembler eventResourceAssembler;

  public CompetitionController(
      CompetitionService competitionService,
      CompetitionResourceAssembler resourceAssembler,
      TeamService teamService,
      TeamResourceAssembler teamResourceAssembler,
      EventService eventService,
      EventResourceAssembler eventResourceAssembler) {

    this.competitionService = competitionService;
    this.resourceAssembler = resourceAssembler;
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
  @RequestMapping(
      value = {"", "/"},
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CollectionModel<CompetitionResource> fetchAllCompetitions() {
    return resourceAssembler.toCollectionModel(competitionService.fetchAll());
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
        .map(resourceAssembler::toModel)
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
      @PathVariable final UUID competitionId) {

    return ResponseEntity.ok(
        eventResourceAssembler.toModel(eventService.fetchEventsForCompetition(competitionId)));
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
  public CollectionModel<TeamResource> fetchCompetitionTeams(
      @PathVariable final UUID competitionId) {

    return teamResourceAssembler.toCollectionModel(
        teamService.fetchTeamsByCompetitionId(competitionId));
  }

  @RequestMapping(
      value = "/competition/{competitionId}/update",
      method = RequestMethod.PATCH,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CompetitionResource> updateCompetition(
      @RequestBody Competition competition) {
    final Competition update = competitionService.update(competition);
    final CompetitionResource resource = resourceAssembler.toModel(update);
    return ResponseEntity.ok(resource);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public String handleIllegalArg(@NotNull IllegalArgumentException e) {
    return e.getMessage();
  }

  @ExceptionHandler(UnknownEntityException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public String handleUnknownEntity(@NotNull UnknownEntityException e) {
    return e.getMessage();
  }
}
