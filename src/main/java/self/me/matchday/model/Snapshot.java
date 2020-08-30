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

package self.me.matchday.model;

import java.time.Instant;
import java.util.function.Function;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class Snapshot<T> {

  private final Instant timestamp;
  private final T data;

  public Snapshot(T data) {
    this.data = data;
    this.timestamp = Instant.now();
  }
  // Copy constructor
  public Snapshot(T data, Instant timestamp) {
    this.data = data;
    this.timestamp = timestamp;
  }

  public <U> Snapshot<U> map(@NotNull final Function<T, U> mapper) {
    return new Snapshot<>(mapper.apply(data));
  }
}
