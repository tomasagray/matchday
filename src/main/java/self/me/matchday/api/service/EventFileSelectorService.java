/*
 * Copyright (c) 2021.
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
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFile.EventPartIdentifier;
import self.me.matchday.model.EventFileSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventFileSelectorService {

  /**
   * Get the "best" file source, sorted by: language -> bitrate -> resolution
   *
   * @param event The event containing the file sources
   * @return The "best" file source
   */
  public EventFileSource getBestFileSource(@NotNull final Event event) {

    // sort file sources
    final ArrayList<EventFileSource> fileSources = new ArrayList<>(event.getFileSources());
    fileSources.sort(
        Comparator.comparing(EventFileSource::getResolution)
            .thenComparing(EventFileSource::getBitrate)
            .thenComparing(EventFileSource::getLanguages));
    // todo - get "preferred" language instead of alphabetical sort ^
    // get top result
    return fileSources.get(0);
  }

  /**
   * Get the best version of each EventFile for this EventFileSource, and return them in the correct
   * order.
   *
   * @param fileSource The source of video data for this Event
   * @return The "best" versions of each EventFile
   */
  public List<EventFile> getPlaylistFiles(@NotNull final EventFileSource fileSource) {

    // Sort EventFiles by part
    final LinkedMultiValueMap<EventPartIdentifier, EventFile> eventParts =
        new LinkedMultiValueMap<>();
    fileSource
        .getEventFiles()
        .forEach(eventFile -> eventParts.add(eventFile.getTitle(), eventFile));

    // Get best version of each part
    return eventParts.values().stream()
        .map(this::getBestEventFile)
        .sorted()
        .collect(Collectors.toList());
  }

  /**
   * Given a List of EventFiles which all represent the same portion of the same event, get the
   * "best" version. Best is determined by: Does the EventFile have an internal URL? ... more to
   * come
   *
   * @param eventFiles A List of EventFile versions
   * @return The "best" version
   */
  private @NotNull EventFile getBestEventFile(@NotNull final List<EventFile> eventFiles) {

    final Optional<EventFile> eventFileOptional =
        eventFiles.stream()
            .min(
                (ev1, ev2) -> {
                  // todo - include file server differentiation
                  if (ev2.getInternalUrl() != null && ev1.getInternalUrl() == null) {
                    return 1;
                  } else if (ev1.getInternalUrl() != null && ev2.getInternalUrl() == null) {
                    return -1;
                  } else if (ev1.getInternalUrl() != null && ev2.getInternalUrl() != null) {
                    return -1;
                  } else {
                    return 0;
                  }
                });
    // Validation should have occurred upstream
    assert eventFileOptional.isPresent();
    // Return "best" event file option
    return eventFileOptional.get();
  }
}
