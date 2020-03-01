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
import self.me.matchday.api.resource.HighlightShowResource;
import self.me.matchday.api.resource.HighlightShowResource.HighlightResourceAssembler;
import self.me.matchday.db.HighlightShowRepository;
import self.me.matchday.model.HighlightShow;
import self.me.matchday.util.Log;

@RestController
public class HighlightShowController extends EventController {

  private static final String LOG_TAG = "HighlightShowController";

  private final HighlightShowRepository highlightShowRepository;
  private final HighlightResourceAssembler highlightResourceAssembler;

  @Autowired
  public HighlightShowController(HighlightShowRepository highlightShowRepository,
      HighlightResourceAssembler highlightResourceAssembler) {

    this.highlightShowRepository = highlightShowRepository;
    this.highlightResourceAssembler = highlightResourceAssembler;
  }

  /**
   * Returns a ResponseEntity representing all Highlight Shows in the DB.
   *
   * @return A Collection of HighlightShows.
   */
  @RequestMapping(value = "/highlight-shows", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<CollectionModel<HighlightShowResource>> fetchAllHighlights() {

    Log.i(LOG_TAG, "Fetching all Highlight Shows");
    final List<HighlightShow> highlightShows = highlightShowRepository.findAll();
    if (highlightShows.size() > 0) {
      // Sort in reverse chronological order
      highlightShows.sort((o1, o2) -> (o1.getDate().compareTo(o2.getDate())) * -1);
      // return DTO
      return new ResponseEntity<>(highlightResourceAssembler.toCollectionModel(highlightShows),
          HttpStatus.OK);
    } else {
      Log.d(LOG_TAG, "Attempting to retrieve all Highlight Shows, but none found");
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
  }

  @RequestMapping(value = "/highlight-shows/highlight/{eventId}", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<HighlightShowResource> fetchHighlightById(@PathVariable Long eventId) {

    Log.i(LOG_TAG, "Fetching Highlight Show with ID: " + eventId);

    return
        highlightShowRepository
            .findById(eventId)
            .map(highlightResourceAssembler::toModel)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }
}
