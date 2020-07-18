package self.me.matchday.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
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
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event;
import self.me.matchday.model.Event.EventSorter;
import self.me.matchday.model.HighlightShow;
import self.me.matchday.model.Match;
import self.me.matchday.util.Log;

@Service
public class EventService {

  private static final String LOG_TAG = "EventService";
  private static final EventSorter EVENT_SORTER = new EventSorter();

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

  /* ===============================================================================================
   * Getters
   * ============================================================================================ */

  public Optional<CollectionModel<EventResource>> fetchAllEvents() {

    Log.i(LOG_TAG, "Fetching latest Events...");

    Optional<CollectionModel<EventResource>> result = Optional.empty();
    final List<Event> events = eventRepository.findAll();
    if (events.size() > 0) {
      events.sort(EVENT_SORTER);
      result = Optional.of(eventResourceAssembler.toCollectionModel(events));
    }
    return result;
  }

  public Optional<Event> fetchById(@NotNull final String eventId) {

    return
        eventRepository.findById(eventId);
  }

  // TODO: Add get event file src by id

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
      matches.sort(EVENT_SORTER);
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

    Log.i(LOG_TAG, "Fetching all Highlight Shows from the database.");
    // Retrieve highlights from database
    final List<HighlightShow> highlightShows = highlightShowRepository.findAll();

    if (highlightShows.size() > 0) {
      // Sort in reverse chronological order
      highlightShows.sort(EVENT_SORTER);
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

  /* ===============================================================================================
   * Setters
   * ============================================================================================ */

  /**
   * Persist an Event; must pass validation, or will skip and make a note in logs.
   *
   * @param event The Event to be saved
   */
  public void saveEvent(@NotNull final Event event) {

    if (isValidEvent(event)) {
      // See if Event already exists in DB
      final Optional<Event> eventOptional = fetchById(event.getEventId());
      // Merge EventFileSources
      eventOptional.ifPresent(value -> event.getFileSources().addAll(value.getFileSources()));
      // Save to DB
      Log.i(LOG_TAG, "Saving event: " + eventRepository.saveAndFlush(event));
    } else {
      Log.d(LOG_TAG, String.format("Event: %s was not saved to DB; invalid", event));
    }
  }

  /**
   * Ensure Event meets certain criteria.
   *
   * @param event The Event to be validated
   * @return True/false - Is a valid Event
   */
  private boolean isValidEvent(final Event event) {

    // Criteria
    boolean titleValid = false,
        competitionValid = false,
        dateValid = false;
    // Minimum date
    final LocalDateTime MIN_DATE =
        LocalDateTime.of(LocalDate.ofYearDay(1970, 1), LocalTime.MIN);

    if (event != null) {
      // Validate title
      final String title = event.getTitle();
      if (title != null && !("".equals(title))) {
        titleValid = true;
      }
      // Validate Competition
      final Competition competition = event.getCompetition();
      if (competition != null) {
        final String name = competition.getName();
        if (name != null && !("".equals(name))) {
          competitionValid = true;
        }
      }
      // Validate date
      final LocalDateTime date = event.getDate();
      if (date != null && date.isAfter(MIN_DATE)) {
        dateValid = true;
      }
    }

    // Perform test
    return
        titleValid && competitionValid && dateValid;
  }
}
