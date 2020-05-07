package self.me.matchday.api.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.EventResource;
import self.me.matchday.api.service.EventService;

@RestController
public class RootController {

  private final EventService eventService;

  @Autowired
  public RootController(EventService eventService) {
    this.eventService = eventService;
  }

  // Link relation identifiers
  private static final LinkRelation EVENTS_REL = LinkRelation.of("events");
  private static final LinkRelation MATCHES_REL = LinkRelation.of("matches");
  private static final LinkRelation HIGHLIGHTS_REL = LinkRelation.of("highlights");
  private static final LinkRelation TEAMS_REL = LinkRelation.of("teams");
  private static final LinkRelation COMPETITIONS_REL = LinkRelation.of("competitions");

  /**
   * Container for root endpoint.
   */
  @Data
  private static class RootResource {

    private CollectionModel<EventResource> featuredEvents;
    public void setFeaturedEvents(@NotNull CollectionModel<EventResource> featuredEvents) {
      this.featuredEvents = featuredEvents;
    }
  }

  @RequestMapping(value = "/", method = RequestMethod.GET)
  public ResponseEntity<EntityModel<RootResource>> root() {

    // Create root endpoint
    final RootResource rootResource = new RootResource();
    // Attach featured Events
//    eventService.fetchFeaturedEvents().ifPresent(rootResource::setFeaturedEvents);
    EntityModel<RootResource> model = new EntityModel<>(rootResource);
    // attach top-level links
    model.add(linkTo(methodOn(EventController.class).fetchAllEvents()).withRel(EVENTS_REL));
    model.add(linkTo(methodOn(EventController.class).fetchAllMatches()).withRel(MATCHES_REL));
    model.add(linkTo(methodOn(EventController.class).fetchAllHighlights()).withRel(
        HIGHLIGHTS_REL));
    model.add(linkTo(methodOn(TeamController.class).fetchAllTeams()).withRel(TEAMS_REL));
    model.add(linkTo(methodOn(CompetitionController.class).fetchAllCompetitions()).withRel(
        COMPETITIONS_REL));

    return new ResponseEntity<>(model, HttpStatus.OK);
  }
}
