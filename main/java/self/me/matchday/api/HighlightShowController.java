/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.db.EventFileSrcRepository;
import self.me.matchday.db.EventSourceRepository;
import self.me.matchday.db.HighlightShowRepository;
import self.me.matchday.feed.EventFileSource;
import self.me.matchday.feed.EventSource;
import self.me.matchday.io.RemoteEventDataManager;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.HighlightShow;
import self.me.matchday.model.SimpleM3U;
import self.me.matchday.model.VariantM3U;
import self.me.matchday.util.Log;

@RestController
public class HighlightShowController extends EventController {

  private static final String LOG_TAG = "HighlightShowController";

  private final HighlightShowRepository highlightShowRepository;
  private final EventSourceRepository eventSourceRepository;
  private final EventFileSrcRepository eventFileSrcRepository;

  public HighlightShowController(
      HighlightShowRepository highlightShowRepository,
      EventSourceRepository eventSourceRepository,
      EventFileSrcRepository eventFileSrcRepository) {
    this.highlightShowRepository = highlightShowRepository;
    this.eventSourceRepository = eventSourceRepository;
    this.eventFileSrcRepository = eventFileSrcRepository;
  }

  /**
   * Fetch all Highlight Shows from the DB and return in reverse chronological order.
   *
   * @return ALl Highlight Shows, newest first
   */
  @GetMapping("/highlight-shows")
  HttpEntity<List<HighlightShowResource>> fetchAllHighlightShows() {

    Log.i(LOG_TAG, "Fetching all HighlightShows");

    // Result container
    final List<HighlightShowResource> highlightShowResources = new ArrayList<>();
    final List<HighlightShow> highlightShows = highlightShowRepository.findAll();

    if (highlightShows.size() > 0) {
      // Sort by date (descending)
      highlightShows.sort(
          (highlightShow, t1) -> (highlightShow.getDate().compareTo(t1.getDate())) * -1);
      // Populate result container
      highlightShows.forEach(
          highlightShow -> highlightShowResources.add(createHighlightShowResource(highlightShow)));

      return new ResponseEntity<>(highlightShowResources, HttpStatus.OK);
    } else {
      Log.d(LOG_TAG, "Attempted to fetch all Highlight Shows, but nothing was found.");
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Fetch a single Highlight Show, specified by the Event ID
   *
   * @param highlightShowId The Event identifier for the desired Highlight Show
   * @return The Highlight Show, as a ResponseEntity
   */
  @GetMapping("/highlight-shows/highlights/{highlightShowId}")
  HttpEntity<HighlightShowResource> fetchHighlightShowById(
      @PathVariable final Long highlightShowId) {

    Log.i(LOG_TAG, "Fetching HighlightShow with ID: " + highlightShowId);

    final Optional<HighlightShow> highlightOptional =
        highlightShowRepository.findById(highlightShowId);
    if (highlightOptional.isPresent()) {
      return new ResponseEntity<>(
          createHighlightShowResource(highlightOptional.get()), HttpStatus.OK);
    } else {
      Log.d(LOG_TAG, "Could not find Highlight Show with ID: " + highlightShowId);
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Fetch the Variant Playlist for this Highlight Show
   *
   * @param highlightShowId The identifier for the Highlight Show
   * @return A Variant Playlist for this Highlight Show
   */
  @GetMapping("/highlight-shows/highlights/{highlightShowId}/play")
  HttpEntity<String> fetchHighlightVariantPlaylist(
      @PathVariable("highlightShowId") final Long highlightShowId) {

    Log.i(LOG_TAG, "Fetching variant Playlist for Highlight Show: " + highlightShowId);

    // Fetch EventSource for this Show
    final Optional<EventSource> eventSourceOptional =
        eventSourceRepository.findSourceByEventId(highlightShowId);
    if (eventSourceOptional.isPresent()) {
      // Create playlist & return
      final VariantM3U variantM3U = new VariantM3U(eventSourceOptional.get());
      return new ResponseEntity<>(
          variantM3U.getPlaylistAsString(), getPlaylistHeaders(), HttpStatus.OK);
    }

    Log.d(LOG_TAG, "Could not create variant playlist for Highlight Show: " + highlightShowId);
    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
  }

  /**
   * Returns a Simple Playlist, given the HighlightShow ID and Variant ID
   *
   * @param highlightShowId The ID of the HighlightShow for this Playlist
   * @param variantId The ID of this specific Playlist
   * @return A Simple Playlist, for direct play of remote video files
   */
  @GetMapping("/highlight-shows/highlights/{highlightShowId}/play/{srcId}/{variantId}.m3u8")
  HttpEntity<String> fetchHighlightSimplePlaylist(
      @PathVariable("highlightShowId") final Long highlightShowId,
      @PathVariable("srcId") final Long srcId,
      @PathVariable("variantId") final String variantId) {

    Log.i(
        LOG_TAG,
        String.format(
            "Getting simple Playlist for Event: %s, Variant: %s", highlightShowId, variantId));

    // First obtain Highlight Show metadata from local DB
    final Optional<HighlightShow> highlightShowOptional =
        highlightShowRepository.findById(highlightShowId);
    if (highlightShowOptional.isPresent()) {

      final HighlightShow highlightShow = highlightShowOptional.get();
      // Get file sources for this Event
      final Optional<EventFileSource> fileSourceOptional =
          eventFileSrcRepository.findFileSrcByEventId(highlightShowId, srcId);
      if (fileSourceOptional.isPresent()) {

        // Fetch EventFiles
        final List<EventFile> eventFiles =
            RemoteEventDataManager.getInstance().getEventFiles(fileSourceOptional.get());
        // Create Simple Playlist & return
        final SimpleM3U playlist = new SimpleM3U(highlightShow, eventFiles);
        return new ResponseEntity<>(
            playlist.getPlaylistAsString(), getPlaylistHeaders(), HttpStatus.OK);
      }
    }

    Log.d(
        LOG_TAG,
        String.format(
            "Could not create simple playlist for Highlight Show: %s, Variant: %s",
            highlightShowId, variantId));
    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
  }

  /**
   * Helper method to construct the Highlight Show resource with appropriate links
   *
   * @param highlightShow The Highlight Show to be wrapped
   * @return The Highlight Show resource, with all links
   */
  @NotNull
  private static HighlightShowResource createHighlightShowResource(
      @NotNull HighlightShow highlightShow) {

    final HighlightShowResource highlightShowResource = new HighlightShowResource(highlightShow);
    // add links
    highlightShowResource
        .add(
            linkTo(
                    methodOn(HighlightShowController.class)
                        .fetchHighlightShowById(highlightShow.getEventId()))
                .withSelfRel())
        .add(
            linkTo(
                    methodOn(CompetitionController.class)
                        .fetchCompetitionById(highlightShow.getCompetition().getCompetitionId()))
                .withRel(COMP_REL))
        .add(
            linkTo(
                    methodOn(HighlightShowController.class)
                        .fetchHighlightVariantPlaylist(highlightShow.getEventId()))
                .withRel(PLAYLIST_REL));

    return highlightShowResource;
  }
}
