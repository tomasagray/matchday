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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.EventResource;
import self.me.matchday.api.resource.EventResource.EventResourceAssembler;
import self.me.matchday.api.service.EventService;

@RestController
public class EventController {

  private final EventService eventService;
  private final EventResourceAssembler resourceAssembler;

  @Autowired
  EventController(final EventService eventService, final EventResourceAssembler resourceAssembler) {
    this.eventService = eventService;
    this.resourceAssembler = resourceAssembler;
  }

  @ResponseBody
  @RequestMapping(value = "/events", method = RequestMethod.GET)
  public CollectionModel<EventResource> fetchAllEvents() {
    return resourceAssembler.toCollectionModel(eventService.fetchAll());
  }
}
