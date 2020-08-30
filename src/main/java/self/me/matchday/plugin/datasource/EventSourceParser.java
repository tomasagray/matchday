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

package self.me.matchday.plugin.datasource;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFileSource;

public interface EventSourceParser {

  /**
   * Return a properly initialized Event
   *
   * @return An Event
   */
  Event getEvent();

  /**
   * Event factory method.
   *
   * @param eventMetadataParser Parser containing Event metadata (Competition, Teams, etc.)
   * @param fileSourceParser File source data parser
   * @return A complete Event with File Sources
   */
  static @NotNull Event createEvent(@NotNull final EventMetadataParser eventMetadataParser,
      @NotNull final EventFileSourceParser fileSourceParser) {

    // Extract data from parsers
    final Event event = eventMetadataParser.getEvent();
    final List<EventFileSource> eventFileSources = fileSourceParser.getEventFileSources();

    // Add file sources to Event & return
    event.addFileSources(eventFileSources);
    return event;
  }
}