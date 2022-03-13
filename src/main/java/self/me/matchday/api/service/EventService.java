/*
 * Copyright (c) 2022.
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
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.EventRepository;
import self.me.matchday.db.VideoFileSrcRepository;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event;
import self.me.matchday.model.Event.EventSorter;
import self.me.matchday.model.ProperName;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.util.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@Transactional
public class EventService {

  private static final String LOG_TAG = "EventService";
  private static final EventSorter EVENT_SORTER = new EventSorter();

  private final EventRepository eventRepository;
  private final VideoFileSrcRepository fileSrcRepository;
  private final EntityCorrectionService entityCorrectionService;

  EventService(
      final EventRepository eventRepository,
      final VideoFileSrcRepository fileSrcRepository,
      EntityCorrectionService entityCorrectionService) {
    this.eventRepository = eventRepository;
    this.fileSrcRepository = fileSrcRepository;
    this.entityCorrectionService = entityCorrectionService;
  }

  // Getters   ==============================================================
  // todo - don't use optional
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

  public Optional<Event> fetchById(@NotNull final UUID eventId) {
    return eventRepository.findById(eventId);
  }

  public Optional<VideoFileSource> fetchVideoFileSrc(final UUID fileSrcId) {
    return fileSrcRepository.findById(fileSrcId);
  }

  /**
   * Retrieve all Events for a given Competition.
   *
   * @param competitionId The ID of the Competition.
   * @return A CollectionModel containing all Events for the specified Competition.
   */
  public Optional<List<Event>> fetchEventsForCompetition(@NotNull final UUID competitionId) {
    return eventRepository.fetchEventsByCompetition(competitionId);
  }

  /**
   * Retrieve all Events associated with the specified Team.
   *
   * @param teamId The name of the Team.
   * @return A CollectionModel containing the Events.
   */
  public Optional<List<Event>> fetchEventsForTeam(@NotNull final UUID teamId) {
    // todo - remove Optional
    return eventRepository.fetchEventsByTeam(teamId);
  }

  // Setters   ==============================================================

  /**
   * Persist an Event; must pass validation, or will skip and make a note in logs.
   *
   * @param event The Event to be saved
   */
  public void saveEvent(@NotNull final Event event) {

    try {
      validateEvent(event);
      entityCorrectionService.correctEntityFields(event);
      // See if Event already exists in DB
      final Optional<Event> eventOptional = eventRepository.findOne(getExampleEvent(event));
      if (eventOptional.isPresent()) {
        final Event existingEvent = eventOptional.get();
        existingEvent.getFileSources().addAll(event.getFileSources());
      } else {
        eventRepository.saveAndFlush(event);
      }
    } catch (Exception e) {
      Log.e(LOG_TAG, String.format("Event: %s was not saved to DB; %s", event, e.getMessage()), e);
    }
  }

  private @NotNull Example<Event> getExampleEvent(@NotNull Event event) {

    final Event exampleEvent =
        Event.builder()
            .competition(event.getCompetition())
            .homeTeam(event.getHomeTeam())
            .awayTeam(event.getAwayTeam())
            .season(event.getSeason())
            .fixture(event.getFixture())
            .build();
    return Example.of(exampleEvent);
  }

  /**
   * Delete the given Event from the database
   *
   * @param event The Event to delete
   */
  public void deleteEvent(@NotNull final Event event) {
    Log.i(LOG_TAG, "Deleting Event with ID: " + event.getEventId());
    eventRepository.delete(event);
  }

  /**
   * Ensure Event meets certain criteria.
   *
   * @param event The Event to be validated
   */
  private void validateEvent(final Event event) {

    if (event == null) {
      reject("Event is null");
    }

    final Competition competition = event.getCompetition();
    if (!isValidCompetition(competition)) {
      reject("invalid competition: " + competition);
    }
    final LocalDateTime date = event.getDate();
    if (!isValidDate(date)) {
      reject("invalid date: " + date);
    }
    final Set<VideoFileSource> fileSources = event.getFileSources();
    if (!isValidVideoFiles(fileSources)) {
      reject("no video files!");
    }
  }

  private void reject(@NotNull final String message) {
    throw new IllegalArgumentException("Event rejected; " + message);
  }

  private boolean isValidCompetition(final Competition competition) {
    if (competition != null) {
      final ProperName name = competition.getProperName();
      return name != null && !("".equals(name.getName()));
    }
    return false;
  }

  private boolean isValidDate(final LocalDateTime date) {
    final LocalDateTime MIN_DATE = LocalDateTime.of(LocalDate.ofYearDay(1970, 1), LocalTime.MIN);
    return date != null && date.isAfter(MIN_DATE);
  }

  private boolean isValidVideoFiles(final Collection<VideoFileSource> fileSources) {
    if (fileSources == null) {
      return false;
    }
    int totalVideoFiles = 0;
    for (final VideoFileSource fileSource : fileSources) {
      totalVideoFiles += fileSource.getVideoFilePacks().size();
    }
    return totalVideoFiles > 0;
  }
}
