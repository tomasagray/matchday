/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.api.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.MatchResource;
import self.me.matchday.api.resource.MatchResource.MatchResourceAssembler;
import self.me.matchday.db.MatchRepository;
import self.me.matchday.model.Match;
import self.me.matchday.util.Log;

@RestController
public class MatchController extends EventController {

  private static final String LOG_TAG = "MatchController";

  private final MatchRepository matchRepository;
  private final MatchResourceAssembler matchResourceAssembler;

  @Autowired
  public MatchController(MatchRepository matchRepository,
      MatchResourceAssembler matchResourceAssembler) {

    this.matchRepository = matchRepository;
    this.matchResourceAssembler = matchResourceAssembler;
  }

  /**
   * Fetch all Matches from the local DB
   *
   * @return A List of Matches as an HttpEntity
   */
  @RequestMapping(value = "/matches", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<CollectionModel<MatchResource>> fetchAllMatches() {

    Log.i(LOG_TAG, "Fetching all Matches");
    final List<Match> matches = matchRepository.findAll();
    if (matches.size() > 0) {
      // Sort by date (descending)
      matches.sort((match, t1) -> (match.getDate().compareTo(t1.getDate())) * -1);
      // return DTOs
      return new ResponseEntity<>(matchResourceAssembler.toCollectionModel(matches), HttpStatus.OK);
    } else {
      Log.d(LOG_TAG, "Attempting to retrieve all Matches, but none found");
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
  }

  /**
   * Fetch a specific Match from the local DB, specified by the Match ID
   *
   * @param matchId Identifier for the Match
   * @return A Match as an HttpEntity
   */
  @RequestMapping(value = "/matches/match/{matchId}", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<MatchResource> fetchMatch(@PathVariable Long matchId) {

    Log.i(LOG_TAG, "Fetching Match with ID: " + matchId);

    // Retrieve Match from local DB
    return
        matchRepository
            .findById(matchId)
            .map(matchResourceAssembler::toModel)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }
}
