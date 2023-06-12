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

package self.me.matchday.unit.plugin.datasource.parsing.fabric;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import self.me.matchday.plugin.datasource.parsing.fabric.Bolt;
import self.me.matchday.unit.plugin.datasource.parsing.fabric.model.Bar;
import self.me.matchday.unit.plugin.datasource.parsing.fabric.model.Bubble;
import self.me.matchday.unit.plugin.datasource.parsing.fabric.model.Foo;
import self.me.matchday.unit.plugin.datasource.parsing.fabric.model.Marklar;

@DisplayName(
    "Testing for the DefaultBolt (default concretion of) fluent interface for Stream zipping & folding")
class DefaultBoltTest {

  private static final Logger logger = LogManager.getLogger(DefaultBoltTest.class);

  @Test
  @DisplayName("Test fluent interface for folding & zipping streams")
  void testFluentStreamFolding() {

    final Stream<Foo> foos = FabricTestDataCreator.getFoos();
    final Stream<Bar> bars = FabricTestDataCreator.getBars();
    final Stream<Marklar> marklars = FabricTestDataCreator.getMarklars();
    final Stream<Bubble> bubbles = FabricTestDataCreator.getBubbles();

    final Stream<Marklar> fizzyMarklars =
        Bolt.of(foos)
            .zipWith(bars, Foo::setBar)
            .zipInto(marklars, Marklar::setFoo)
            .foldWith(bubbles, new BubbleFolder<>(), Marklar::addBubbles)
            .stream();

    fizzyMarklars.forEach(
        marklar -> {
          logger.info("Got Fizzy Marklar:\n" + marklar);
          assertThat(marklar).isNotNull();
          assertThat(marklar.getBubbles().size()).isEqualTo(FabricTestDataCreator.MAX_BUBBLES);
        });
  }

  @Test
  @DisplayName("Test folding & zipping of streams in reverse order")
  void reverseZipAndFold() {

    final Stream<Foo> foos = FabricTestDataCreator.getFoos();
    final Stream<Bar> bars = FabricTestDataCreator.getBars();
    final Stream<Marklar> marklars = FabricTestDataCreator.getMarklars();
    final Stream<Bubble> bubbles = FabricTestDataCreator.getBubbles();

    final Stream<Marklar> fizzyMarklars =
        Bolt.of(bubbles)
            .foldInto(marklars, new BubbleFolder<>(), Marklar::addBubbles)
            .zipWith(foos, Marklar::setFoo)
            .zipWith(bars, (marklar, bar) -> marklar.getFoo().setBar(bar))
            .stream();

    fizzyMarklars.forEach(
        marklar -> {
          logger.info("Got Fizzy Marklar:\n" + marklar);
          assertThat(marklar).isNotNull();
          assertThat(marklar.getBubbles().size()).isEqualTo(FabricTestDataCreator.MAX_BUBBLES);
          logger.info("Reverse zipping & folding works! Eureka!");
        });
  }

  @Test
  @DisplayName("Test zipping & folding of generics")
  void testGenericFabric() {

    final int START = 0;
    final int END = 10;
    final int LOTTA_BUBBLES = 100;

    final List<? extends Foo> genericFoos =
        IntStream.range(START, END).mapToObj(i -> new Foo(String.format("f:[%s]", i))).toList();
    final List<? extends Bar> genericBars =
        IntStream.range(START, END).mapToObj(i -> new Bar(String.format("b:{%s}", i))).toList();
    final List<? extends Marklar> genericMarklars =
        IntStream.range(START, END)
            .mapToObj(i -> new Marklar(String.format("M:((%s))", i)))
            .toList();
    final List<? extends Bubble> genericBubbles =
        IntStream.range(START, LOTTA_BUBBLES)
            .mapToObj(i -> new Bubble(String.format("b:%s", i)))
            .toList();

    final Stream<? extends Marklar> genericFizzyMarklars =
        Bolt.of(genericFoos.stream())
            .zipWith(genericBars.stream(), Foo::setBar)
            .zipInto(genericMarklars.stream(), Marklar::setFoo)
            .foldWith(genericBubbles.stream(), new BubbleFolder<>(), Marklar::addBubbles)
            .stream();

    genericFizzyMarklars.forEach(
        marklar -> {
          logger.info("Got generic Fizzy Marklar:\n" + marklar);
          assertThat(marklar).isNotNull();
          assertThat(marklar.getBubbles().size()).isEqualTo(FabricTestDataCreator.MAX_BUBBLES);
        });
  }
}
