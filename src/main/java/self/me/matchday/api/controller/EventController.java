package self.me.matchday.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.HighlightShowResource;
import self.me.matchday.api.resource.MatchResource;
import self.me.matchday.api.service.EventService;
import self.me.matchday.util.Log;

@RestController
public class EventController {

  private static final String LOG_TAG = "EventController";

  private final EventService eventService;

  @Autowired
  EventController(EventService eventService) {
    this.eventService = eventService;
  }

  /**
   * Fetch all Matches from local DB and return as a response entity.
   *
   * @return A List of Matches as an HttpEntity.
   */
  @RequestMapping(value = "/matches", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<CollectionModel<MatchResource>> fetchAllMatches() {

    return
        eventService
            .fetchAllMatches()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
  }

  /**
   * Fetch a specific Match from the local DB, specified by the Match ID
   *
   * @param matchId Identifier for the Match
   * @return A Match as an HttpEntity
   */
  @RequestMapping(value = "/matches/match/{matchId}", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<MatchResource> fetchMatch(@PathVariable String matchId) {

    return
        eventService
            .fetchMatch(matchId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Returns a ResponseEntity representing all Highlight Shows in the DB.
   *
   * @return A Collection of HighlightShows.
   */
  @RequestMapping(value = "/highlight-shows", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<CollectionModel<HighlightShowResource>> fetchAllHighlights() {

    return
        eventService
            .fetchAllHighlightShows()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(value = "/highlight-shows/highlight/{eventId}", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<HighlightShowResource> fetchHighlightById(@PathVariable String eventId) {

    Log.i(LOG_TAG, "Fetching Highlight Show with ID: " + eventId);

    return
        eventService
            .fetchHighlightShow(eventId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }
}