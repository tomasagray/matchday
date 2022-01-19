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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import self.me.matchday.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testing for the Fabric Stream manipulation library")
class FabricTest {

  private static final String LOG_TAG = "FabricTest";
  private static final int START_IDX = 0;
  private static final int END_IDX = 5;
  private static final int MAX_BUBBLES = 4;
  private static final Folder<Bubble, List<Bubble>> bubbleFolder =
      new Folder<>() {

        @Contract(pure = true)
        @Override
        public @NotNull Supplier<List<Bubble>> identity() {
          return ArrayList::new;
        }

        @Contract(pure = true)
        @Override
        public @NotNull BiFunction<Bubble, List<Bubble>, List<Bubble>> accumulator() {
          return (bubble, bubbles) -> {
            //              Log.d(LOG_TAG, "Adding bubble: " + bubble);
            bubbles.add(bubble);
            //              Log.d(LOG_TAG, "accumulator is now:\n" + bubbles);
            return bubbles;
          };
        }

        @Contract(pure = true)
        @Override
        public boolean isAccumulatorFull(@NotNull List<Bubble> accumulator) {
          return accumulator.size() >= MAX_BUBBLES;
        }

        @Contract(pure = true)
        @Override
        public @Nullable Function<List<Bubble>, List<Bubble>> finisher() {
          return null;
        }
      };

  private static Stream<Foo> getFoos() {
    return IntStream.range(START_IDX, END_IDX).mapToObj(i -> "Foo[" + i + "]").map(Foo::new);
  }

  private static Stream<Bar> getBars() {
    return IntStream.range(START_IDX, END_IDX).mapToObj(i -> "Bar{" + i + "}").map(Bar::new);
  }

  private static Stream<Marklar> getMarklars() {
    return IntStream.range(START_IDX, END_IDX)
        .mapToObj(i -> "Marklar((" + i + "))")
        .map(Marklar::new);
  }

  private static Stream<Bubble> getBubbles() {
    final int maxBubbles = 20;
    return IntStream.range(START_IDX, maxBubbles)
        .mapToObj(i -> "Bubble%" + i + "%")
        .map(Bubble::new);
  }

  private static @NotNull Stream<Foo> getZippedFoos() {
    final Stream<Foo> foos = getFoos();
    final Stream<Bar> bars = getBars();
    return Fabric.zip(
        foos,
        bars,
        (foo, bar) -> {
          foo.setBar(bar);
          return foo;
        });
  }

  private static @NotNull Stream<Marklar> getZippedMarklars() {
    final Stream<Marklar> marklars = getMarklars();
    final Stream<Foo> zippedFoos = getZippedFoos();
    return Fabric.zip(
        marklars,
        zippedFoos,
        (marklar, foo) -> {
          marklar.setFoo(foo);
          return marklar;
        });
  }

  @Test
  @DisplayName("Test 1:1 Stream zipping")
  void zip() {

    AtomicInteger zippedFooCount = new AtomicInteger(0);
    final Stream<Foo> zippedFoos = getZippedFoos();
    zippedFoos.forEach(
        foo -> {
          Log.i(LOG_TAG, "Got zipped Foo: " + foo);
          assertThat(foo).isNotNull();
          zippedFooCount.getAndIncrement();
        });

    final int count = zippedFooCount.get();
    Log.i(LOG_TAG, "Total Zipped Foo count: " + count);
    assertThat(count).isEqualTo(END_IDX);
  }

  @Test
  @DisplayName("Test multi-Stream zip")
  void testMultiStreamZip() {

    AtomicInteger zippedMarklarCount = new AtomicInteger(0);
    final Stream<Marklar> zippedMarklars = getZippedMarklars();
    zippedMarklars.forEach(
        marklar -> {
          Log.i(LOG_TAG, "Got zipped Marklar:\n" + marklar);
          assertThat(marklar).isNotNull();
          zippedMarklarCount.getAndIncrement();
        });

    final int count = zippedMarklarCount.get();
    Log.i(LOG_TAG, "Zipped Marklar count: " + count);
    assertThat(count).isEqualTo(END_IDX);
  }

  @Test
  @DisplayName("Test Stream folding")
  void fold() {

    final Stream<Bubble> bubbleStream = getBubbles();

    AtomicInteger foldedBubbleCount = new AtomicInteger(0);
    final Stream<List<Bubble>> foldedBubbles = Fabric.fold(bubbleStream, bubbleFolder);
    foldedBubbles.forEach(
        bubbles -> {
          Log.i(LOG_TAG, "Got folded Bubbles:\n" + bubbles);
          assertThat(bubbles).isNotNull().isNotEmpty();
          assertThat(bubbles.size()).isEqualTo(MAX_BUBBLES);
          foldedBubbleCount.getAndIncrement();
        });

    final int count = foldedBubbleCount.get();
    Log.i(LOG_TAG, "Folded Bubble count: " + count);
    assertThat(count).isEqualTo(END_IDX);
  }

  @Test
  @DisplayName("Test Stream folding & zipping")
  void testFoldingAndZipping() {

    final Stream<Bubble> bubbleStream = getBubbles();
    final Stream<List<Bubble>> foldedBubbles = Fabric.fold(bubbleStream, bubbleFolder);
    final Stream<Marklar> zippedMarklars = getZippedMarklars();
    final Stream<Marklar> zippedAndFolded =
        Fabric.zip(
            zippedMarklars,
            foldedBubbles,
            (marklar, bubbles) -> {
              marklar.addBubbles(bubbles);
              return marklar;
            });

    zippedAndFolded.forEach(
        marklar -> {
          Log.i(LOG_TAG, "Zipped & Folded Marklar:\n" + marklar);
          assertThat(marklar).isNotNull();
          assertThat(marklar.getBubbles().size()).isEqualTo(MAX_BUBBLES);
        });
  }

  private static class Foo {

    private final String name;
    private Bar bar;

    public Foo(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    public void setBar(Bar bar) {
      this.bar = bar;
    }

    public String toString() {
      return String.format("FooName: %s; FooBar: %s", name, bar);
    }
  }

  private static class Bar {

    private final String title;

    public Bar(String title) {
      this.title = title;
    }

    public String getTitle() {
      return title;
    }

    public String toString() {
      return this.getTitle();
    }
  }

  private static class Marklar {

    private final String name;
    private final List<Bubble> bubbles = new ArrayList<>();
    private Foo foo;

    public Marklar(String name) {
      this.name = name;
    }

    public void setFoo(Foo foo) {
      this.foo = foo;
    }

    public void addBubbles(List<Bubble> bubbles) {
      this.bubbles.addAll(bubbles);
    }

    @Contract(pure = true)
    public @NotNull @UnmodifiableView List<Bubble> getBubbles() {
      return Collections.unmodifiableList(bubbles);
    }

    public String toString() {
      return String.format("Marklar[%s]'s Foo: %s;; Bubbles: %s", name, foo, bubbles);
    }
  }

  private static class Bubble {

    private final String name;

    public Bubble(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public String toString() {
      return "Bubble{" + "name='" + name + "'" + '}';
    }
  }
}
