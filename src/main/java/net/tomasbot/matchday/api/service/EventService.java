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

package net.tomasbot.matchday.api.service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.tomasbot.matchday.api.service.video.VideoStreamingService;
import net.tomasbot.matchday.db.EventRepository;
import net.tomasbot.matchday.model.Competition;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.Event.EventSorter;
import net.tomasbot.matchday.model.Highlight;
import net.tomasbot.matchday.model.Match;
import net.tomasbot.matchday.model.video.VideoFileSource;
import net.tomasbot.matchday.model.video.VideoPlaylist;
import net.tomasbot.matchday.model.video.VideoStreamLocatorPlaylist;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EventService implements EntityService<Event, UUID> {

  public static final Sort DEFAULT_EVENT_SORT = Sort.by(Direction.DESC, "date");
  private static final EventSorter EVENT_SORTER = new EventSorter();
  private final EventRepository eventRepository;
  private final MatchService matchService;
  private final HighlightService highlightService;
  private final CompetitionService competitionService;
  private final VideoStreamingService streamingService;

  EventService(
      EventRepository eventRepository,
      MatchService matchService,
      HighlightService highlightService,
      CompetitionService competitionService,
      VideoStreamingService streamingService) {
    this.eventRepository = eventRepository;
    this.matchService = matchService;
    this.highlightService = highlightService;
    this.competitionService = competitionService;
    this.streamingService = streamingService;
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

  @Override
  public Optional<Event> fetchById(@NotNull final UUID eventId) {
    return eventRepository.findById(eventId).map(this::initialize);
  }

  @Override
  public List<Event> fetchAll() {
    final List<Event> events = eventRepository.findAll();
    if (!events.isEmpty()) {
      events.forEach(this::initialize);
      events.sort(EVENT_SORTER);
    }
    return events;
  }

  public Page<Event> fetchAllPaged(final int page, final int size) {
    final PageRequest request = PageRequest.of(page, size, DEFAULT_EVENT_SORT);
    final Page<Event> eventPage = eventRepository.findAll(request);
    eventPage.forEach(this::initialize);
    return eventPage;
  }

  /**
   * Retrieve all Events for a given Competition.
   *
   * @param competitionId The ID of the Competition.
   * @return A CollectionModel containing all Events for the specified Competition.
   */
  public Page<Event> fetchEventsForCompetition(
      @NotNull final UUID competitionId, final int page, final int size) {
    final PageRequest request = PageRequest.of(page, size, DEFAULT_EVENT_SORT);
    final Page<Event> events = eventRepository.fetchEventsByCompetition(competitionId, request);
    events.forEach(this::initialize);
    return events;
  }

  public Optional<Collection<VideoFileSource>> fetchVideoFileSources(@NotNull UUID eventId) {
    return fetchById(eventId).map(Event::getFileSources);
  }

  public Optional<Event> fetchEventLike(@NotNull Event event) {
    return eventRepository.findOne(Example.of(event));
  }

  /**
   * Persist an Event; must pass validation, or will skip and make a note in logs.
   *
   * @param event The Event to be saved
   */
  @Override
  public Event save(@NotNull final Event event) {
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

  public Optional<VideoPlaylist> getPreferredPlaylist(@NotNull UUID eventId) {
    return fetchById(eventId).flatMap(streamingService::getBestVideoStreamPlaylist);
  }

  public Optional<VideoPlaylist> getVideoStreamPlaylist(
      @NotNull UUID eventId, @NotNull UUID fileSrcId) {
    return fetchById(eventId)
        .flatMap(event -> streamingService.getOrCreateVideoStreamPlaylist(event, fileSrcId));
  }

  public VideoFileSource updateVideoFileSource(
      @NotNull UUID eventId, @NotNull VideoFileSource source) {
    return fetchVideoFileSources(eventId)
        .map(sources -> streamingService.addOrUpdateVideoSource(sources, source))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Trying to update Video source for non-existent Event: " + eventId));
  }

  public void deleteVideoFileSource(@NotNull UUID eventId, @NotNull UUID fileSrcId)
      throws IOException {
    Optional<Event> eventOptional = fetchById(eventId);
    if (eventOptional.isPresent()) {
      Set<VideoFileSource> fileSources = eventOptional.get().getFileSources();
      fileSources.removeIf(source -> fileSrcId.equals(source.getFileSrcId()));

      Optional<VideoStreamLocatorPlaylist> playlistOptional =
          streamingService.getPlaylistForFileSource(fileSrcId);
      if (playlistOptional.isPresent()) {
        VideoStreamLocatorPlaylist playlist = playlistOptional.get();
        streamingService.deleteAllVideoData(playlist);
      }
    } else {
      throw new IllegalArgumentException("Event does not exist: " + eventId);
    }
  }
}
