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
import self.me.matchday.plugin.datasource.parsing.fabric.model.Bar;
import self.me.matchday.plugin.datasource.parsing.fabric.model.Bubble;
import self.me.matchday.plugin.datasource.parsing.fabric.model.Foo;
import self.me.matchday.plugin.datasource.parsing.fabric.model.Marklar;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FabricTestDataCreator {

  public static final int START_IDX = 0;
  public static final int END_IDX = 5;
  public static final int MAX_BUBBLES = 4;

  public static Stream<Foo> getFoos() {
    return IntStream.range(START_IDX, END_IDX).mapToObj(i -> "Foo[" + i + "]").map(Foo::new);
  }

  public static Stream<Bar> getBars() {
    return IntStream.range(START_IDX, END_IDX).mapToObj(i -> "Bar{" + i + "}").map(Bar::new);
  }

  public static Stream<Marklar> getMarklars() {
    return IntStream.range(START_IDX, END_IDX)
        .mapToObj(i -> "Marklar((" + i + "))")
        .map(Marklar::new);
  }

  public static Stream<Bubble> getBubbles() {
    final int maxBubbles = 20;
    return IntStream.range(START_IDX, maxBubbles)
        .mapToObj(i -> "Bubble%" + i + "%")
        .map(Bubble::new);
  }

  public static @NotNull Stream<Foo> getZippedFoos() {
    final Stream<Foo> foos = getFoos();
    final Stream<Bar> bars = getBars();
    return Fabric.zip(foos, bars, Foo::setBar);
  }

  public static @NotNull Stream<Marklar> getZippedMarklars() {
    final Stream<Marklar> marklars = getMarklars();
    final Stream<Foo> zippedFoos = getZippedFoos();
    return Fabric.zip(marklars, zippedFoos, Marklar::setFoo);
  }
}
