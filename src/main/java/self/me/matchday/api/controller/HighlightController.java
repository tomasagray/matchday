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
import self.me.matchday.api.service.HighlightService;
import self.me.matchday.util.Log;

@RestController
@RequestMapping(value = "/highlights")
public class HighlightController {

  private static final String LOG_TAG = "HighlightController";
  private final HighlightService highlightService;
  private final EventResourceAssembler resourceAssembler;

  @Autowired
  public HighlightController(final HighlightService highlightService,
      final EventResourceAssembler resourceAssembler) {

    this.highlightService = highlightService;
    this.resourceAssembler = resourceAssembler;
  }

  /**
   * Returns a ResponseEntity representing all Highlight Shows in the DB.
   *
   * @return A Collection of Highlights.
   */
  @RequestMapping(value = "/highlight-shows", method = RequestMethod.GET)
  @ResponseBody
  public CollectionModel<EventResource> fetchAllHighlights() {

    return
        highlightService
            .fetchAllHighlights()
            .map(resourceAssembler::toCollectionModel)
            .orElse(null);
  }

  /**
   * Fetch a specific Highlight from the local database.
   *
   * @param eventId The ID of the Highlight.
   * @return A ResponseEntity containing the requested Highlight.
   */
  @RequestMapping(value = "/highlight-shows/highlight/{eventId}", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<EventResource> fetchHighlightById(@PathVariable String eventId) {

    Log.i(LOG_TAG, "Fetching Highlight Show with ID: " + eventId);

    return
        highlightService
            .fetchHighlight(eventId)
            .map(resourceAssembler::toModel)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }
}
