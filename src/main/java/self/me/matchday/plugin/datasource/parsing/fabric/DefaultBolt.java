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

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class DefaultBolt<T> implements Bolt<T> {

  private final Stream<T> stream;

  DefaultBolt(Stream<T> stream) {
    this.stream = stream;
  }

  @Override
  public <R> Bolt<T> zipWith(@NotNull Stream<R> stream, BiConsumer<T, R> combiner) {
    final Stream<T> zippedStream = Fabric.zip(this.stream, stream, combiner);
    return new DefaultBolt<>(zippedStream);
  }

  @Override
  public <R> Bolt<R> zipInto(@NotNull Stream<R> stream, BiConsumer<R, T> combiner) {
    final Stream<R> zippedStream = Fabric.zip(stream, this.stream, combiner);
    return new DefaultBolt<>(zippedStream);
  }

  @Override
  public <R, A> Bolt<A> zipWithBecoming(@NotNull Stream<R> stream, BiFunction<R, T, A> combiner) {
    final Stream<A> zippedStream = Fabric.zip(stream, this.stream, combiner);
    return new DefaultBolt<>(zippedStream);
  }

  @Override
  public <R, A> Bolt<T> foldWith(
      @NotNull Stream<R> stream, Folder<R, A> folder, BiConsumer<T, A> combiner) {

    final Stream<A> foldedStream = Fabric.fold(stream, folder);
    final Stream<T> zippedFoldedStream = Fabric.zip(this.stream, foldedStream, combiner);
    return new DefaultBolt<>(zippedFoldedStream);
  }

  @Override
  public <R, A> Bolt<R> foldInto(
      @NotNull Stream<R> stream, Folder<T, A> folder, BiConsumer<R, A> combiner) {

    final Stream<A> foldedStream = Fabric.fold(this.stream, folder);
    final Stream<R> zippedFoldedStream = Fabric.zip(stream, foldedStream, combiner);
    return new DefaultBolt<>(zippedFoldedStream);
  }

  @Override
  public Stream<T> stream() {
    return this.stream;
  }
}
