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

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.api.service.video.VideoStreamingService;
import self.me.matchday.db.EventRepository;
import self.me.matchday.db.VideoFileSrcRepository;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event;
import self.me.matchday.model.Event.EventSorter;
import self.me.matchday.model.Highlight;
import self.me.matchday.model.Match;
import self.me.matchday.model.ProperName;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoPlaylist;

@Service
@Transactional
public class EventService implements EntityService<Event, UUID> {

  private static final EventSorter EVENT_SORTER = new EventSorter();

  private final EventRepository eventRepository;
  private final VideoFileSrcRepository fileSrcRepository;
  private final MatchService matchService;
  private final HighlightService highlightService;
  private final CompetitionService competitionService;
  private final VideoStreamingService videoStreamingService;

  EventService(
      EventRepository eventRepository,
      EntityCorrectionService entityCorrectionService,
      MatchService matchService,
      HighlightService highlightService,
      CompetitionService competitionService,
      VideoFileSrcRepository fileSrcRepository,
      VideoStreamingService videoStreamingService) {
    this.eventRepository = eventRepository;
    this.fileSrcRepository = fileSrcRepository;
    this.matchService = matchService;
    this.highlightService = highlightService;
    this.competitionService = competitionService;
    this.videoStreamingService = videoStreamingService;
  }

  @Override
  public Event initialize(@NotNull Event event) {
    final Competition competition = event.getCompetition();
    if (competition != null) {
      competitionService.initialize(competition);
    }
    if (event instanceof Match) {
      matchService.initialize((Match) event);
    } else if (event instanceof Highlight) {
      highlightService.initialize((Highlight) event);
    }
    Hibernate.initialize(event.getFileSources());
    Hibernate.initialize(event.getArtwork());
    return event;
  }

  /**
   * Persist an Event; must pass validation, or will skip and make a note in logs.
   *
   * @param event The Event to be saved
   */
  @Override
  public Event save(@NotNull final Event event) {

    validateEvent(event);
    Event saved;
    if (event instanceof Match) {
      saved = matchService.save((Match) event);
    } else if (event instanceof Highlight) {
      saved = highlightService.save((Highlight) event);
    } else {
      throw new IllegalArgumentException(
          "Trying to save unknown type: " + event.getClass().getName());
    }
    initialize(saved);
    return saved;
  }

  @Override
  public List<Event> saveAll(@NotNull Iterable<? extends Event> entities) {
    return StreamSupport.stream(entities.spliterator(), false)
        .map(this::save)
        .collect(Collectors.toList());
  }

  @Override
  public List<Event> fetchAll() {
    final List<Event> events = eventRepository.findAll();
    if (events.size() > 0) {
      events.forEach(this::initialize);
      events.sort(EVENT_SORTER);
    }
    return events;
  }

  @Override
  public Event update(@NotNull Event event) {
    if (event instanceof Match) {
      return matchService.update((Match) event);
    } else if (event instanceof Highlight) {
      return highlightService.update((Highlight) event);
    }
    throw new IllegalArgumentException("Could not determine type for Event: " + event);
  }

  @Override
  public List<Event> updateAll(@NotNull Iterable<? extends Event> events) {
    return StreamSupport.stream(events.spliterator(), false)
        .map(this::update)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Event> fetchById(@NotNull final UUID eventId) {
    return eventRepository.findById(eventId).map(this::initialize);
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
  public List<Event> fetchEventsForCompetition(@NotNull final UUID competitionId) {
    return eventRepository.fetchEventsByCompetition(competitionId).stream()
        .map(this::initialize)
        .collect(Collectors.toList());
  }

  public Optional<VideoPlaylist> getBestVideoStreamPlaylist(@NotNull UUID eventId) {
    return fetchById(eventId)
        .flatMap(videoStreamingService::getBestVideoStreamPlaylist)
        .flatMap(
            playlist -> getVideoStreamPlaylist(playlist.getEventId(), playlist.getFileSrcId()));
  }

  public Optional<VideoPlaylist> getVideoStreamPlaylist(
      @NotNull UUID eventId, @NotNull UUID fileSrcId) {
    return fetchById(eventId)
        .flatMap(event -> videoStreamingService.getVideoStreamPlaylist(event, fileSrcId));
  }

  public Optional<Collection<VideoFileSource>> fetchVideoFileSources(@NotNull UUID eventId) {
    return fetchById(eventId).map(Event::getFileSources);
  }

  public Resource getVideoSegmentResource(@NotNull Long partId, @NotNull String segmentId) {
    return videoStreamingService.getVideoSegmentResource(partId, segmentId);
  }

  public void deleteVideoData(@NotNull UUID fileSrcId) throws IOException {
    videoStreamingService.deleteVideoData(fileSrcId);
  }

  public int killAllStreamingTasks() {
    return videoStreamingService.killAllStreamingTasks();
  }

  public Optional<String> readPlaylistFile(@NotNull Long partId) {
    return videoStreamingService.readPlaylistFile(partId);
  }

  /**
   * Delete the given Event from the database
   *
   * @param eventId The ID of the Event to delete
   */
  @Override
  public void delete(@NotNull final UUID eventId) {
    eventRepository.deleteById(eventId);
  }

  @Override
  public void deleteAll(@NotNull Iterable<? extends Event> events) {
    eventRepository.deleteAll(events);
  }

  /**
   * Ensure Event meets certain criteria.
   *
   * @param event The Event to be validated
   */
  private void validateEvent(final Event event) {

    if (event == null) {
      throw new IllegalArgumentException("Event is null");
    }
    final Competition competition = event.getCompetition();
    if (!isValidCompetition(competition)) {
      throw new InvalidEventException("invalid competition: " + competition);
    }
    final LocalDateTime date = event.getDate();
    if (!isValidDate(date)) {
      throw new InvalidEventException("invalid date: " + date);
    }
    final Collection<VideoFileSource> fileSources = event.getFileSources();
    if (!isValidVideoFiles(fileSources)) {
      throw new InvalidEventException("Event has no video files");
    }
  }

  private boolean isValidCompetition(final Competition competition) {
    if (competition != null) {
      final ProperName name = competition.getName();
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
