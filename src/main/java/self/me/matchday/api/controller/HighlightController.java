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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.EventsResource;
import self.me.matchday.api.resource.EventsResource.EventsModeller;
import self.me.matchday.api.resource.HighlightResource;
import self.me.matchday.api.service.HighlightService;
import self.me.matchday.model.Highlight;

@RestController
@RequestMapping(value = "/highlights")
public class HighlightController {

  private final HighlightService highlightService;
  private final EventsModeller eventAssembler;
  private final HighlightResource.HighlightResourceAssembler highlightAssembler;

  @Autowired
  public HighlightController(
      HighlightService highlightService,
      EventsModeller eventAssembler,
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
  public ResponseEntity<EventsResource> fetchAllHighlights(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "16") int size) {
    final Page<Highlight> highlightPage = highlightService.fetchAll(page, size);
    final EventsResource model = eventAssembler.toModel(highlightPage.getContent());
    if (highlightPage.hasNext()) {
      model.add(
          linkTo(
                  methodOn(HighlightController.class)
                      .fetchAllHighlights(highlightPage.getNumber() + 1, size))
              .withRel("next"));
    }
    return ResponseEntity.ok(model);
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
