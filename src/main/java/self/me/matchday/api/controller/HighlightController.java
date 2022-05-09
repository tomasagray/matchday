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
import self.me.matchday.api.service.HighlightService;
import self.me.matchday.util.Log;

import java.util.UUID;

@RestController
@RequestMapping(value = "/highlights")
public class HighlightController {

  private static final String LOG_TAG = "HighlightController";
  private final HighlightService highlightService;
  private final EventResourceAssembler resourceAssembler;

  @Autowired
  public HighlightController(
      final HighlightService highlightService, final EventResourceAssembler resourceAssembler) {

    this.highlightService = highlightService;
    this.resourceAssembler = resourceAssembler;
  }

  /**
   * Returns a ResponseEntity representing all Highlight Shows in the DB.
   *
   * @return A Collection of Highlights.
   */
  @RequestMapping(value = "/highlight-shows", method = RequestMethod.GET)
  @ResponseBody
  public CollectionModel<EventResource> fetchAllHighlights() {
    return resourceAssembler.toCollectionModel(highlightService.fetchAllHighlights());
  }

  /**
   * Fetch a specific Highlight from the local database.
   *
   * @param eventId The ID of the Highlight.
   * @return A ResponseEntity containing the requested Highlight.
   */
  @RequestMapping(value = "/highlight-shows/highlight/{eventId}", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<EventResource> fetchHighlightById(@PathVariable UUID eventId) {

    Log.i(LOG_TAG, "Fetching Highlight Show with ID: " + eventId);

    return highlightService
        .fetchHighlight(eventId)
        .map(resourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
