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

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class FolderSpliterator<R, T> extends Spliterators.AbstractSpliterator<R> {

  private final Spliterator<T> spliterator;
  private final long streamSize;
  private final AtomicInteger currentElement = new AtomicInteger(0);
  private final Folder<T, R> folder;
  private final AtomicReference<R> ref;

  public FolderSpliterator(@NotNull Spliterator<T> spliterator, @NotNull Folder<T, R> folder) {
    super(spliterator.estimateSize(), spliterator.characteristics());

    this.spliterator = spliterator;
    this.streamSize = spliterator.estimateSize();
    this.folder = folder;
    ref = new AtomicReference<>(folder.identity().get());
  }

  @Override
  public boolean tryAdvance(Consumer<? super R> action) {

    synchronized (currentElement) {
      return spliterator.tryAdvance(
          t -> {
            R r = ref.get();
            final BiConsumer<T, R> accumulator = folder.accumulator();
            final boolean lastElement = currentElement.incrementAndGet() >= this.streamSize;
            final boolean accumulatorFull = folder.isAccumulatorFull().test(t, r);
            if (!accumulatorFull) {
              accumulator.accept(t, r);
            } else {
              // add accumulator to Stream
              action.accept(r);
              // reset accumulator
              final R r2 = folder.identity().get();
              accumulator.accept(t, r2);
              ref.setRelease(r2);
            }
            // ensure last element pushed through
            if (lastElement) {
              action.accept(ref.get());
            }
          });
    }
  }
}
