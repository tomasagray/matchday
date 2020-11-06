/*
 * Copyright (c) 2020.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.api.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.db.EventFileSrcRepository;
import self.me.matchday.db.EventRepository;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event;
import self.me.matchday.model.Event.EventSorter;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.util.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EventService {

  private static final String LOG_TAG = "EventService";
  private static final EventSorter EVENT_SORTER = new EventSorter();

  private final EventRepository eventRepository;
  private final EventFileSrcRepository fileSrcRepository;

  @Autowired
  EventService(final EventRepository eventRepository,
      final EventFileSrcRepository fileSrcRepository) {

    this.eventRepository = eventRepository;
    this.fileSrcRepository = fileSrcRepository;
  }

  // ==============
  // Getters
  // ==============

  public Optional<List<Event>> fetchAllEvents() {

    Log.i(LOG_TAG, "Fetching latest Events...");

    // Fetch Events from database
    final List<Event> events = eventRepository.findAll();
    // Sort Events
    if (events.size() > 0) {
      events.sort(EVENT_SORTER);
      return Optional.of(events);
    }
    // None found
    return Optional.empty();
  }

  public Optional<Event> fetchById(@NotNull final String eventId) {

    return
        eventRepository.findById(eventId);
  }

  public Optional<EventFileSource> fetchEventFileSrc(@NotNull final UUID fileSrcId) {

    return
        fileSrcRepository.findById(fileSrcId);
  }

  /**
   * Retrieve all Events for a given Competition.
   *
   * @param competitionId The ID of the Competition.
   * @return A CollectionModel containing all Events for the specified Competition.
   */
  public Optional<List<Event>> fetchEventsForCompetition(
      @NotNull final String competitionId) {

    return
        eventRepository
            .fetchEventsByCompetition(competitionId);
  }

  /**
   * Retrieve all Events associated with the specified Team.
   *
   * @param teamId The ID of the Team.
   * @return A CollectionModel containing the Events.
   */
  public Optional<List<Event>> fetchEventsForTeam(@NotNull final String teamId) {

    return
        eventRepository
            .fetchEventsByTeam(teamId);
  }

  // ==============
  // Setters
  // ==============

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
