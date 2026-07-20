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

package net.tomasbot.matchday.model;

import java.time.Instant;
import java.util.stream.Stream;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Getter
public class Snapshot<T> {

  private final Instant timestamp;
  private final Stream<T> data;
  private int dataCount;

  public Snapshot(Stream<T> data) {
    this(data, Instant.now());
  }

  public Snapshot(Stream<T> data, Instant timestamp) {
    this.data = data;
    this.timestamp = timestamp;
  }

  public Snapshot(Stream<T> data, int dataCount) {
    this(data);
    this.dataCount = dataCount;
  }

  @Contract("_ -> new")
  public static <T> @NotNull Snapshot<T> of(@NotNull Stream<T> data) {
    return new Snapshot<>(data);
  }

  public static <T> @NotNull Snapshot<T> of(@NotNull Stream<T> data, int dataCount) {
    return new Snapshot<>(data, dataCount);
  }

  public String toString() {
    return "Snapshot(" + "timestamp=" + timestamp + ", dataCount=" + dataCount + ')';
  }
}
