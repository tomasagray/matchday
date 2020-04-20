package self.me.matchday.api.service;

import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Service;
import self.me.matchday.api.resource.EventResource;
import self.me.matchday.api.resource.EventResource.EventResourceAssembler;
import self.me.matchday.api.resource.HighlightShowResource;
import self.me.matchday.api.resource.HighlightShowResource.HighlightResourceAssembler;
import self.me.matchday.api.resource.MatchResource;
import self.me.matchday.api.resource.MatchResource.MatchResourceAssembler;
import self.me.matchday.db.EventRepository;
import self.me.matchday.db.HighlightShowRepository;
import self.me.matchday.db.MatchRepository;
import self.me.matchday.model.Event;
import self.me.matchday.model.HighlightShow;
import self.me.matchday.model.Match;
import self.me.matchday.util.Log;

@Service
public class EventService {

  private static final String LOG_TAG = "EventService";

  private final MatchRepository matchRepository;
  private final HighlightShowRepository highlightShowRepository;
  private final EventRepository eventRepository;
  private final EventResourceAssembler eventResourceAssembler;
  private final MatchResourceAssembler matchResourceAssembler;
  private final HighlightResourceAssembler highlightResourceAssembler;

  @Autowired
  EventService(MatchRepository matchRepository, MatchResourceAssembler matchResourceAssembler,
      EventRepository eventRepository, EventResourceAssembler eventResourceAssembler,
      HighlightShowRepository highlightShowRepository,
      HighlightResourceAssembler highlightResourceAssembler) {

    this.matchRepository = matchRepository;
    this.eventRepository = eventRepository;
    this.eventResourceAssembler = eventResourceAssembler;
    this.matchResourceAssembler = matchResourceAssembler;
    this.highlightShowRepository = highlightShowRepository;
    this.highlightResourceAssembler = highlightResourceAssembler;
  }

  /**
   * Fetch the 3 most recent Events.
   *
   * @return A CollectionModel of Events.
   */
  public Optional<CollectionModel<EventResource>> fetchFeaturedEvents() {

    Log.i(LOG_TAG, "Fetching featured Events.");
    final int EVENT_COUNT = 3;

    // Get latest 3 events from database
    final Optional<List<Event>> eventOptional = eventRepository
        .fetchLatestEvents(PageRequest.of(0, EVENT_COUNT));
    if (eventOptional.isPresent()) {
      final List<Event> events = eventOptional.get();
      return Optional.of(eventResourceAssembler.toCollectionModel(events));
    } else {
      Log.i(LOG_TAG, "Attempted to retrieve featured Events, but none found.");
      return Optional.empty();
    }
  }

  /**
   * Retrieve all Matches from the repo (database) and assemble into a collection of resources.
   *
   * @return Collection of assembled resources.
   */
  public Optional<CollectionModel<MatchResource>> fetchAllMatches() {

    Log.i(LOG_TAG, "Fetching all Matches from database.");
    // Retrieve all matches from repo
    final List<Match> matches = matchRepository.findAll();

    if (matches.size() > 0) {
      // Sort by date (descending)
      matches.sort((match, t1) -> (match.getDate().compareTo(t1.getDate())) * -1);
      // return DTOs
      return Optional.of(matchResourceAssembler.toCollectionModel(matches));
    } else {
      Log.d(LOG_TAG, "Attempting to retrieve all Matches, but none found");
      return Optional.empty();
    }
  }

  /**
   * Retrieve a specific match from the local DB.
   *
   * @param matchId The ID of the match we want.
   * @return An optional containing the match resource, if it was found.
   */
  public Optional<MatchResource> fetchMatch(@NotNull String matchId) {

    Log.i(LOG_TAG, String.format("Fetching Match with ID: %s from the database.", matchId));
    return
        matchRepository
            .findById(matchId)
            .map(matchResourceAssembler::toModel);
  }

  /**
   * Retrieve all Events for a given Competition.
   *
   * @param competitionId The ID of the Competition.
   * @return A CollectionModel containing all Events for the specified Competition.
   */
  public Optional<CollectionModel<EventResource>> fetchEventsForCompetition(
      @NotNull final String competitionId) {

    return
        eventRepository
            .fetchEventsByCompetition(competitionId)
            .map(eventResourceAssembler::toCollectionModel);
  }

  /**
   * Retrieve all Events associated with the specified Team.
   *
   * @param teamId The ID of the Team.
   * @return A CollectionModel containing the Events.
   */
  public Optional<CollectionModel<EventResource>> fetchEventsForTeam(@NotNull final String teamId) {

    return
        eventRepository
            .fetchEventsByTeam(teamId)
            .map(eventResourceAssembler::toCollectionModel);
  }

  /**
   * Retrieve all Highlight Shows from the database.
   *
   * @return Optional collection model of highlight show resources.
   */
  public Optional<CollectionModel<HighlightShowResource>> fetchAllHighlightShows() {

    Log.i(LOG_TAG, "Fetching all Highlight Shows from database.");
    // Retrieve highlights from database
    final List<HighlightShow> highlightShows = highlightShowRepository.findAll();

    if (highlightShows.size() > 0) {
      // Sort in reverse chronological order
      highlightShows.sort((o1, o2) -> (o1.getDate().compareTo(o2.getDate())) * -1);
      // return DTO
      return Optional.of(highlightResourceAssembler.toCollectionModel(highlightShows));
    } else {
      Log.d(LOG_TAG, "Attempting to retrieve all Highlight Shows, but none found");
      return Optional.empty();
    }
  }

  /**
   * Retrieve a specific HighlightShow from the database.
   *
   * @param highlightShowId ID of the Highlight Show.
   * @return The requested HighlightShow, or empty().
   */
  public Optional<HighlightShowResource> fetchHighlightShow(@NotNull String highlightShowId) {

    Log.i(LOG_TAG, "Fetching Highlight Show for ID: " + highlightShowId);
    return
        highlightShowRepository
            .findById(highlightShowId)
            .map(highlightResourceAssembler::toModel);
  }
}
