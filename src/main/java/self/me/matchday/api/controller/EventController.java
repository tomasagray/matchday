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

    return
        eventService
          .fetchAllEvents()
          .map(resourceAssembler::toCollectionModel)
          .orElse(null);
  }
}
