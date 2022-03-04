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

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Fabric {

  public static <L, R> @NotNull Stream<L> zip(
      @NotNull Stream<L> leftStream, @NotNull Stream<R> rightStream, BiConsumer<L, R> combiner) {

    Spliterator<L> leftSplit = leftStream.spliterator();
    Spliterator<R> rightSplit = rightStream.spliterator();
    final long minSize = Long.min(leftSplit.estimateSize(), rightSplit.estimateSize());
    final int characteristics = leftSplit.characteristics() & rightSplit.characteristics();

    return StreamSupport.stream(
        new Spliterators.AbstractSpliterator<>(minSize, characteristics) {

          @Override
          public boolean tryAdvance(Consumer<? super L> action) {

            return leftSplit.tryAdvance(
                left ->
                    rightSplit.tryAdvance(
                        right -> {
                          combiner.accept(left, right);
                          action.accept(left);
                        }));
          }
        },
        leftStream.isParallel() || rightStream.isParallel());
  }

  public static <L, R, A> @NotNull Stream<A> zip(
      @NotNull Stream<L> leftStream, @NotNull Stream<R> rightStream, BiFunction<L, R, A> combiner) {

    Spliterator<L> leftSplit = leftStream.spliterator();
    Spliterator<R> rightSplit = rightStream.spliterator();
    final long minSize = Long.min(leftSplit.estimateSize(), rightSplit.estimateSize());
    final int characteristics = leftSplit.characteristics() & rightSplit.characteristics();

    return StreamSupport.stream(
        new Spliterators.AbstractSpliterator<>(minSize, characteristics) {

          @Override
          public boolean tryAdvance(Consumer<? super A> action) {

            return leftSplit.tryAdvance(
                left ->
                    rightSplit.tryAdvance(
                        right -> {
                          final A a = combiner.apply(left, right);
                          action.accept(a);
                        }));
          }
        },
        leftStream.isParallel() || rightStream.isParallel());
  }

  public static <T, R> Stream<R> fold(@NotNull Stream<T> stream, @NotNull Folder<T, R> folder) {

    final Spliterator<T> spliterator = stream.spliterator();
    final boolean parallel = stream.isParallel();
    final Spliterator<R> folderSpliterator = new FolderSpliterator<>(spliterator, folder);
    return StreamSupport.stream(folderSpliterator, parallel).filter(Objects::nonNull);
  }
}
