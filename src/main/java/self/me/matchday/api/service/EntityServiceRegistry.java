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

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.matchday.model.Event;
import self.me.matchday.model.Match;

@Component
public class EntityServiceRegistry {

  private final List<Entry<?, ?>> registry = new ArrayList<>();

  public EntityServiceRegistry(EventService eventService, MatchService matchService) {
    registry.add(new Entry<>(Event.class, eventService));
    registry.add(new Entry<>(Match.class, matchService));
  }

  public <T, I> void registerService(Class<T> type, EntityService<T, I> service) {
    registry.add(new Entry<>(type, service));
  }

  @SuppressWarnings("unchecked cast")
  public <T, I> EntityService<T, I> getServiceFor(@NotNull Class<T> clazz) {
    return registry.stream()
        .filter(entry -> entry.clazz().equals(clazz))
        .map(entry -> (EntityService<T, I>) entry.service())
        .findAny()
        .orElseThrow(
            () -> new IllegalArgumentException("No Service found for class: " + clazz.getName()));
  }

  private record Entry<T, S extends EntityService<T, ?>>(Class<T> clazz, S service) {}
}
