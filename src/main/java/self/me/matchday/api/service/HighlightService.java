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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.db.HighlightRepository;
import self.me.matchday.model.Event.EventSorter;
import self.me.matchday.model.Highlight;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class HighlightService {

  private static final EventSorter EVENT_SORTER = new EventSorter();

  private final HighlightRepository highlightRepository;

  @Autowired
  public HighlightService(final HighlightRepository highlightRepository) {

    this.highlightRepository = highlightRepository;
  }

  /**
   * Retrieve all Highlight Shows from the database.
   *
   * @return Optional collection model of highlight show resources.
   */
  public List<Highlight> fetchAllHighlights() {

    final List<Highlight> highlights = highlightRepository.findAll();
    if (highlights.size() > 0) {
      highlights.sort(EVENT_SORTER);
    }
    return highlights;
  }

  /**
   * Retrieve a specific Highlight from the database.
   *
   * @param highlightShowId ID of the Highlight Show.
   * @return The requested Highlight, or empty().
   */
  public Optional<Highlight> fetchHighlight(@NotNull UUID highlightShowId) {
    return highlightRepository.findById(highlightShowId);
  }
}
