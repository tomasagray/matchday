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

package self.me.matchday.plugin.datasource.parsing.fabric;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * A fluent interface for combining streams
 *
 * @param <T> The type of the initial Stream
 */
public interface Bolt<T> {

  @Contract(value = "_ -> new", pure = true)
  static <T> @NotNull Bolt<T> of(Stream<T> stream) {
    return new DefaultBolt<>(stream);
  }

  <R> Bolt<T> zipWith(@NotNull Stream<R> stream, BiConsumer<T, R> combiner);

  <R> Bolt<R> zipInto(@NotNull Stream<R> stream, BiConsumer<R, T> combiner);

  <R, A> Bolt<A> zipWithBecoming(@NotNull Stream<R> stream, BiFunction<R, T, A> combiner);

  <R, A> Bolt<T> foldWith(
      @NotNull Stream<R> stream, Folder<R, A> folder, BiConsumer<T, A> combiner);

  <R, A> Bolt<R> foldInto(
      @NotNull Stream<R> stream, Folder<T, A> folder, BiConsumer<R, A> combiner);

  Stream<T> stream();
}
