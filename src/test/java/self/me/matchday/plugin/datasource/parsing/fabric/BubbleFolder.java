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
import self.me.matchday.plugin.datasource.parsing.fabric.model.Bubble;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class BubbleFolder<T extends Bubble> extends Folder<T, List<Bubble>> {

  @Contract(pure = true)
  @Override
  public @NotNull Supplier<List<Bubble>> identity() {
    return ArrayList::new;
  }

  @Contract(pure = true)
  @Override
  public @NotNull BiConsumer<T, List<Bubble>> accumulator() {
    return (bubble, bubbles) -> bubbles.add(bubble);
  }

  @Contract(pure = true)
  @Override
  public BiPredicate<T, List<Bubble>> isAccumulatorFull() {
    return (bubble, bubbles) -> bubbles.size() >= FabricTestDataCreator.MAX_BUBBLES;
  }
}
