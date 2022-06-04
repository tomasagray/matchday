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
import org.springframework.web.bind.annotation.*;
import self.me.matchday.api.resource.EventResource;
import self.me.matchday.api.resource.EventResource.EventResourceAssembler;
import self.me.matchday.api.service.MatchService;

import java.util.UUID;

@RestController
@RequestMapping(value = "/matches")
public class MatchController {

  private final MatchService matchService;
  private final EventResourceAssembler resourceAssembler;

  @Autowired
  public MatchController(
      final MatchService matchService, final EventResourceAssembler resourceAssembler) {

    this.matchService = matchService;
    this.resourceAssembler = resourceAssembler;
  }

  /**
   * Fetch all Matches from local DB and return as a response entity.
   *
   * @return A List of Matches as an HttpEntity.
   */
  @RequestMapping(
      value = {"", "/"},
      method = RequestMethod.GET)
  @ResponseBody
  public CollectionModel<EventResource> fetchAllMatches() {
    return resourceAssembler.toCollectionModel(matchService.fetchAll());
  }

  /**
   * Fetch a specific Match from the local DB, specified by the Match ID
   *
   * @param matchId Identifier for the Match
   * @return A Match as an HttpEntity
   */
  @RequestMapping(value = "/match/{matchId}", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<EventResource> fetchMatchById(@PathVariable UUID matchId) {

    return matchService
        .fetchById(matchId)
        .map(resourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
