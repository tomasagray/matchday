package self.me.matchday.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.EventResource;
import self.me.matchday.api.resource.EventResource.EventResourceAssembler;
import self.me.matchday.api.service.MatchService;

@RestController
@RequestMapping(value = "/matches")
public class MatchController {

  private final MatchService matchService;
  private final EventResourceAssembler resourceAssembler;

  @Autowired
  public MatchController(final MatchService matchService,
      final EventResourceAssembler resourceAssembler) {

    this.matchService = matchService;
    this.resourceAssembler = resourceAssembler;
  }

  /**
   * Fetch all Matches from local DB and return as a response entity.
   *
   * @return A List of Matches as an HttpEntity.
   */
  @RequestMapping(value = "/", method = RequestMethod.GET)
  @ResponseBody
  public CollectionModel<EventResource> fetchAllMatches() {

    return
        matchService
            .fetchAllMatches()
            .map(resourceAssembler::toCollectionModel)
            .orElse(null);
  }

  /**
   * Fetch a specific Match from the local DB, specified by the Match ID
   *
   * @param matchId Identifier for the Match
   * @return A Match as an HttpEntity
   */
  @RequestMapping(value = "/match/{matchId}", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<EventResource> fetchMatchById(@PathVariable String matchId) {

    return
        matchService
            .fetchMatch(matchId)
            .map(resourceAssembler::toModel)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }
}