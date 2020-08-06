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
import self.me.matchday.api.service.EventService;
import self.me.matchday.util.Log;

@RestController
public class EventController {

  private static final String LOG_TAG = "EventController";

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


  /**
   * Fetch all Matches from local DB and return as a response entity.
   *
   * @return A List of Matches as an HttpEntity.
   */
  @RequestMapping(value = "/matches", method = RequestMethod.GET)
  @ResponseBody
  public CollectionModel<EventResource> fetchAllMatches() {

    return
        eventService
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
  @RequestMapping(value = "/matches/match/{matchId}", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<EventResource> fetchMatchById(@PathVariable String matchId) {

    return
        eventService
            .fetchMatch(matchId)
            .map(resourceAssembler::toModel)
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
  public CollectionModel<EventResource> fetchAllHighlights() {

    return
        eventService
            .fetchAllHighlightShows()
            .map(resourceAssembler::toCollectionModel)
            .orElse(null);
  }

  /**
   * Fetch a specific HighlightShow from the local database.
   *
   * @param eventId The ID of the HighlightShow.
   * @return A ResponseEntity containing the requested HighlightShow.
   */
  @RequestMapping(value = "/highlight-shows/highlight/{eventId}", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<EventResource> fetchHighlightById(@PathVariable String eventId) {

    Log.i(LOG_TAG, "Fetching Highlight Show with ID: " + eventId);

    return
        eventService
            .fetchHighlightShow(eventId)
            .map(resourceAssembler::toModel)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }
}
