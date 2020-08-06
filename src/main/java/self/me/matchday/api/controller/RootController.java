package self.me.matchday.api.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

  // Link relation identifiers
  private static final LinkRelation EVENTS_REL = LinkRelation.of("events");
  private static final LinkRelation MATCHES_REL = LinkRelation.of("matches");
  private static final LinkRelation HIGHLIGHTS_REL = LinkRelation.of("highlights");
  private static final LinkRelation TEAMS_REL = LinkRelation.of("teams");
  private static final LinkRelation COMPETITIONS_REL = LinkRelation.of("competitions");

  /**
   * Container for root endpoint.
   */
  private static class RootResource extends RepresentationModel<RootResource> {

  }

  @RequestMapping(value = "/", method = RequestMethod.GET)
  public ResponseEntity<EntityModel<RootResource>> root() {

    // Create root endpoint
    EntityModel<RootResource> root = EntityModel.of(new RootResource());

    // attach top-level links
    root.add(linkTo(
        methodOn(EventController.class)
            .fetchAllEvents())
        .withRel(EVENTS_REL));
    root.add(linkTo(
        methodOn(MatchController.class)
            .fetchAllMatches())
        .withRel(MATCHES_REL));
    root.add(linkTo(
        methodOn(HighlightController.class)
            .fetchAllHighlights())
        .withRel(HIGHLIGHTS_REL));
    root.add(linkTo(
        methodOn(TeamController.class)
            .fetchAllTeams())
        .withRel(TEAMS_REL));
    root.add(linkTo(
        methodOn(CompetitionController.class)
            .fetchAllCompetitions())
        .withRel(COMPETITIONS_REL));

    return new ResponseEntity<>(root, HttpStatus.OK);
  }
}
