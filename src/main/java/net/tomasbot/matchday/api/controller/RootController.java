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

package net.tomasbot.matchday.api.controller;

import static net.tomasbot.matchday.util.Constants.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

  public static final int DEFAULT_PAGE_SIZE = 16;
  public static final int DEFAULT_PAGE = 0;
  
  @RequestMapping(
      value = {"", "/"},
      method = RequestMethod.GET)
  public ResponseEntity<EntityModel<RootResource>> root() {
    // Create root endpoint
    EntityModel<RootResource> root = EntityModel.of(new RootResource());

    // attach top-level links
    root.add(
        linkTo(methodOn(EventController.class).fetchAllEvents(DEFAULT_PAGE, DEFAULT_PAGE_SIZE))
            .withRel(LinkRelations.EVENTS_REL));
    root.add(
        linkTo(methodOn(MatchController.class).fetchAllMatches(DEFAULT_PAGE, DEFAULT_PAGE_SIZE))
            .withRel(LinkRelations.MATCHES_REL));
    root.add(
        linkTo(
                methodOn(HighlightController.class)
                    .fetchAllHighlights(DEFAULT_PAGE, DEFAULT_PAGE_SIZE))
            .withRel(LinkRelations.HIGHLIGHTS_REL));
    root.add(
        linkTo(methodOn(TeamController.class).fetchAllTeams(DEFAULT_PAGE, DEFAULT_PAGE_SIZE))
            .withRel(LinkRelations.TEAMS_REL));
    root.add(
        linkTo(methodOn(CompetitionController.class).fetchAllCompetitions())
            .withRel(LinkRelations.COMPETITIONS_REL));

    return new ResponseEntity<>(root, HttpStatus.OK);
  }

  /** Container for root endpoint. */
  public static class RootResource extends RepresentationModel<RootResource> {}
}
