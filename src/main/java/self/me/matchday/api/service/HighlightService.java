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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import self.me.matchday.db.HighlightRepository;
import self.me.matchday.model.Event.EventSorter;
import self.me.matchday.model.Highlight;

@Service
public class HighlightService implements EntityService<Highlight, UUID> {

  private static final EventSorter EVENT_SORTER = new EventSorter();

  private final HighlightRepository highlightRepository;
  private final EntityCorrectionService entityCorrectionService;

  public HighlightService(
      HighlightRepository highlightRepository, EntityCorrectionService entityCorrectionService) {
    this.highlightRepository = highlightRepository;
    this.entityCorrectionService = entityCorrectionService;
  }

  @Override
  public Highlight initialize(@NotNull Highlight highlight) {
    Hibernate.initialize(highlight);
    return highlight;
  }

  /**
   * Retrieve all Highlight Shows from the database.
   *
   * @return Optional collection model of highlight show resources.
   */
  @Override
  public List<Highlight> fetchAll() {
    final List<Highlight> highlights = highlightRepository.findAll();
    if (!highlights.isEmpty()) {
      highlights.sort(EVENT_SORTER);
      highlights.forEach(this::initialize);
    }
    return highlights;
  }

  public Page<Highlight> fetchAll(final int page, final int size) {
    final PageRequest request = PageRequest.of(page, size, EventService.DEFAULT_EVENT_SORT);
    final Page<Highlight> highlights = highlightRepository.findAll(request);
    highlights.forEach(this::initialize);
    return highlights;
  }

  /**
   * Retrieve a specific Highlight from the database.
   *
   * @param highlightShowId ID of the Highlight Show.
   * @return The requested Highlight, or empty().
   */
  @Override
  public Optional<Highlight> fetchById(@NotNull UUID highlightShowId) {
    return highlightRepository.findById(highlightShowId).map(this::initialize);
  }

  @Override
  public Highlight save(@NotNull Highlight highlight) {
    try {
      entityCorrectionService.correctEntityFields(highlight);
      // See if Event already exists in DB
      final Optional<Highlight> eventOptional =
          highlightRepository.findOne(getExampleEvent(highlight));
      if (eventOptional.isPresent()) {
        final Highlight existingEvent = eventOptional.get();
        existingEvent.addAllFileSources(highlight.getFileSources());
        return existingEvent;
      }
      final Highlight saved = highlightRepository.save(highlight);
      return initialize(saved);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private @NotNull Example<Highlight> getExampleEvent(@NotNull Highlight highlight) {
    final Highlight example =
        Highlight.highlightBuilder()
            .competition(highlight.getCompetition())
            .season(highlight.getSeason())
            .fixture(highlight.getFixture())
            .date(highlight.getDate())
            .build();
    return Example.of(example);
  }

  @Override
  public List<Highlight> saveAll(@NotNull Iterable<? extends Highlight> highlights) {
    return StreamSupport.stream(highlights.spliterator(), false)
        .map(this::save)
        .collect(Collectors.toList());
  }

  @Override
  public Highlight update(@NotNull Highlight highlight) {
    final UUID eventId = highlight.getEventId();
    final Optional<Highlight> optional = fetchById(eventId);
    if (optional.isPresent()) {
      return save(highlight);
    }
    // else..
    throw new IllegalArgumentException(
        "Trying to update non-existent Highlight Show with ID: " + eventId);
  }

  @Override
  public List<Highlight> updateAll(@NotNull Iterable<? extends Highlight> highlights) {
    return StreamSupport.stream(highlights.spliterator(), false)
        .map(this::update)
        .collect(Collectors.toList());
  }

  @Override
  public void delete(@NotNull UUID highlightId) {
    highlightRepository.deleteById(highlightId);
  }

  @Override
  public void deleteAll(@NotNull Iterable<? extends Highlight> highlights) {
    highlightRepository.deleteAll(highlights);
  }
}
