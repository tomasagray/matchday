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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import self.me.matchday.api.resource.EventsResource;
import self.me.matchday.api.resource.EventsResource.EventResourceAssembler;
import self.me.matchday.api.resource.HighlightResource;
import self.me.matchday.api.service.HighlightService;

import java.util.UUID;

@RestController
@RequestMapping(value = "/highlights")
public class HighlightController {

  private final HighlightService highlightService;
  private final EventResourceAssembler eventAssembler;
  private final HighlightResource.HighlightResourceAssembler highlightAssembler;

  @Autowired
  public HighlightController(
      HighlightService highlightService,
      EventResourceAssembler eventAssembler,
      HighlightResource.HighlightResourceAssembler highlightAssembler) {

    this.highlightService = highlightService;
    this.eventAssembler = eventAssembler;
    this.highlightAssembler = highlightAssembler;
  }

  /**
   * Returns a ResponseEntity representing all Highlight Shows in the DB.
   *
   * @return A Collection of Highlights.
   */
  @RequestMapping(value = "/highlight-shows", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<EventsResource> fetchAllHighlights() {
    return ResponseEntity.ok(eventAssembler.toModel(highlightService.fetchAll()));
  }

  /**
   * Fetch a specific Highlight from the local database.
   *
   * @param eventId The ID of the Highlight.
   * @return A ResponseEntity containing the requested Highlight.
   */
  @RequestMapping(value = "/highlight-shows/highlight/{eventId}", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<HighlightResource> fetchHighlightById(@PathVariable UUID eventId) {

    return highlightService
        .fetchById(eventId)
        .map(highlightAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
