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

package self.me.matchday.plugin.datasource.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import self.me.matchday.plugin.datasource.parsing.fabric.Folder;

public class ListFolder<T> extends Folder<T, List<T>> {

  @Override
  public Supplier<List<T>> identity() {
    return ArrayList::new;
  }

  @Override
  public BiConsumer<T, List<T>> accumulator() {
    return (t, list) -> list.add(t);
  }

  @Override
  public BiPredicate<T, List<T>> isAccumulatorFull() {
    return (t, list) -> false; // list holds all elements
  }
}
