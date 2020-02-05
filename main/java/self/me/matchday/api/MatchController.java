/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.db.EventFileSrcRepository;
import self.me.matchday.db.EventSourceRepository;
import self.me.matchday.db.MatchRepository;
import self.me.matchday.feed.EventFileSource;
import self.me.matchday.feed.EventSource;
import self.me.matchday.io.RemoteEventDataManager;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.Match;
import self.me.matchday.model.SimpleM3U;
import self.me.matchday.model.VariantM3U;
import self.me.matchday.util.Log;

@RestController
public class MatchController extends EventController {

  private static final String LOG_TAG = "MatchController";

  private final MatchRepository matchRepository;
  private final EventSourceRepository eventSourceRepository;
  private final EventFileSrcRepository eventFileSrcRepository;

  MatchController(
      MatchRepository matchRepository,
      EventSourceRepository eventSourceRepository,
      EventFileSrcRepository eventFileSrcRepository) {
    this.matchRepository = matchRepository;
    this.eventSourceRepository = eventSourceRepository;
    this.eventFileSrcRepository = eventFileSrcRepository;
  }

  /**
   * Fetch all Matches from the local DB
   *
   * @return A List of Matches as an HttpEntity
   */
  @GetMapping("/matches")
  HttpEntity<List<MatchResource>> fetchAllMatches() {

    Log.i(LOG_TAG, "Fetching all Matches");

    // Return container
    final List<MatchResource> matchResources;
    final List<Match> matches = matchRepository.findAll();
    if (matches.size() > 0) {
      // Sort by date (descending)
      matches.sort((match, t1) -> (match.getDate().compareTo(t1.getDate())) * -1);
      // Add as resources
      matchResources =
          matches.stream().map(MatchController::createMatchResource).collect(Collectors.toList());
      return new ResponseEntity<>(matchResources, HttpStatus.OK);
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
  @GetMapping("/matches/match/{matchId}")
  HttpEntity<MatchResource> fetchMatch(@PathVariable Long matchId) {

    Log.i(LOG_TAG, "Fetching Match with ID: " + matchId);

    // Retrieve Match from local DB
    final Optional<Match> matchOptional = matchRepository.findById(matchId);
    if (matchOptional.isPresent()) {
      return new ResponseEntity<>(createMatchResource(matchOptional.get()), HttpStatus.OK);
    } else {
      Log.d(LOG_TAG, "Could not find Match with ID: " + matchId);
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  // TODO: Add links; client can decide which playlist to play, navigate to Simple playlist directly
  /**
   * Fetch the Variant Playlist for this Match
   *
   * @param matchId The identifier for the Match
   * @return A Playlist for this match
   */
  @GetMapping("/matches/match/{matchId}/play/variant.m3u8")
  HttpEntity<String> fetchMatchVariantPlaylist(@PathVariable("matchId") final Long matchId) {

    Log.i(LOG_TAG, "Fetching Playlist for Event: " + matchId);

    // Get EventSources for this Match
    final Optional<EventSource> eventSourceOptional =
        eventSourceRepository.findSourceByEventId(matchId);
    if (eventSourceOptional.isPresent()) {
      final VariantM3U variantM3U = new VariantM3U(eventSourceOptional.get());
      return new ResponseEntity<>(
          variantM3U.getPlaylistAsString(), getPlaylistHeaders(), HttpStatus.OK);
    }

    // Playlist generation failed
    Log.d(LOG_TAG, "Could not generate Playlist for Match with ID: " + matchId);
    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
  }

  /**
   * Returns a Simple Playlist, given the Match ID and Variant ID
   *
   * @param matchId The ID of the Match for this Playlist
   * @param variantId The ID of this specific Playlist
   * @return A Simple Playlist, for direct play of remote video files
   */
  @GetMapping("/matches/match/{matchId}/play/{srcId}/{variantId}.m3u8")
  HttpEntity<String> fetchMatchSimplePlaylist(
      @PathVariable final Long matchId,
      @PathVariable final Long srcId,
      @PathVariable final String variantId) {

    Log.i(
        LOG_TAG,
        String.format("Getting simple Playlist for Event: %s, Variant: %s", matchId, variantId));

    // First retrieve Event (Match) metadata
    final Optional<Match> matchOptional = matchRepository.findById(matchId);
    if (matchOptional.isPresent()) {

      // Get EventFileSource
      final Optional<EventFileSource> fileSourceOptional =
          eventFileSrcRepository.findFileSrcByEventId(matchId, srcId);
      if (fileSourceOptional.isPresent()) {

        // Retrieve remote files
        final List<EventFile> eventFiles =
            RemoteEventDataManager.getInstance().getEventFiles(fileSourceOptional.get());
        // Generate playlist & return
        final SimpleM3U playlist = new SimpleM3U(matchOptional.get(), eventFiles);
    System.out.println("Playlist sent:\n" + playlist.getPlaylistAsString());
        return
            new ResponseEntity<>(playlist.getPlaylistAsString(), getPlaylistHeaders(), HttpStatus.OK);
      }
    }

    // We could not find requested info
    Log.d(
        LOG_TAG,
        String.format(
            "Could not retrieve Simple Playlist data for Match %s, Variant: (%s) %s",
            matchId, srcId, variantId));
    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
  }

  /**
   * Helper method to standardize creation of the Match resources, with all appropriate links
   *
   * @param match The Match to be wrapped
   * @return The Match resource, with all links
   */
  @NotNull
  private static MatchResource createMatchResource(@NotNull final Match match) {

    final MatchResource matchResource = new MatchResource(match);
    // add links
    matchResource
        .add(linkTo(methodOn(MatchController.class).fetchMatch(match.getEventId())).withSelfRel())
        .add(
            linkTo(methodOn(MatchController.class).fetchMatchVariantPlaylist(match.getEventId()))
                .withRel(PLAYLIST_REL))
        .add(
            linkTo(methodOn(TeamController.class).fetchTeamById(match.getHomeTeam().getTeamId()))
                .withRel(HOME_TEAM_REL))
        .add(
            linkTo(methodOn(TeamController.class).fetchTeamById(match.getAwayTeam().getTeamId()))
                .withRel(AWAY_TEAM_REL))
        .add(
            linkTo(
                    methodOn(CompetitionController.class)
                        .fetchCompetitionById(match.getCompetition().getCompetitionId()))
                .withRel(COMP_REL));

    return matchResource;
  }
}
