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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.model.Event;
import self.me.matchday.model.Snapshot;
import self.me.matchday.util.Log;

import java.util.stream.Stream;

@Service
public class SnapshotService {

  private final EventService eventService;

  public SnapshotService(EventService eventService) {
    this.eventService = eventService;
  }

  @Transactional
  @SuppressWarnings("unchecked cast")
  public <T> void saveSnapshot(@NotNull Snapshot<T> snapshot, @NotNull Class<T> clazz) {

    // todo - implement other Snapshot types
    if (clazz.equals(Event.class)) {
      final Stream<Event> data = (Stream<Event>) snapshot.getData();
      data.peek(event -> Log.i("EVENT", "Got Event: " + event)).forEach(eventService::saveEvent);
    }
  }
}
