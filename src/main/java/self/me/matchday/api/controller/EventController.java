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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.EventsResource;
import self.me.matchday.api.resource.EventsResource.EventsResourceAssembler;
import self.me.matchday.api.service.EventService;
import self.me.matchday.model.Event;

@RestController
public class EventController {

  private final EventService eventService;
  private final EventsResourceAssembler resourceAssembler;

  @Autowired
  EventController(
      final EventService eventService, final EventsResourceAssembler resourceAssembler) {
    this.eventService = eventService;
    this.resourceAssembler = resourceAssembler;
  }

  @ResponseBody
  @RequestMapping(value = "/events", method = RequestMethod.GET)
  public ResponseEntity<EventsResource> fetchAllEvents(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "16") int size) {
    final Page<Event> events = eventService.fetchAllPaged(page, size);
    final EventsResource resource = resourceAssembler.toModel(events.getContent());
    if (events.hasNext()) {
      resource.add(
          linkTo(methodOn(EventController.class).fetchAllEvents(events.getNumber() + 1, size))
              .withRel("next"));
    }
    return ResponseEntity.ok(resource);
  }
}
