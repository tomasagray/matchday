package self.me.matchday.api.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

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
  public ResponseEntity<RootResource> root() {

    // Create root endpoint and attach links to top-level controllers
    final RootResource rootResource = new RootResource();
    rootResource.add(linkTo(methodOn(MatchController.class).fetchAllMatches()).withRel(MATCHES_REL));
    rootResource.add(linkTo(methodOn(HighlightShowController.class).fetchAllHighlights()).withRel(
        HIGHLIGHTS_REL));
    rootResource.add(linkTo(methodOn(TeamController.class).fetchAllTeams()).withRel(TEAMS_REL));
    rootResource.add(linkTo(methodOn(CompetitionController.class).fetchAllCompetitions()).withRel(
        COMPETITIONS_REL));
    return new ResponseEntity<>(rootResource, HttpStatus.OK);
  }
}
