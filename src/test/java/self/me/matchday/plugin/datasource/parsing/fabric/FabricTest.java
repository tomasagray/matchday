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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import self.me.matchday.plugin.datasource.parsing.fabric.model.Bubble;
import self.me.matchday.plugin.datasource.parsing.fabric.model.Foo;
import self.me.matchday.plugin.datasource.parsing.fabric.model.Marklar;
import self.me.matchday.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testing for the Fabric Stream manipulation library")
class FabricTest {

  private static final String LOG_TAG = "FabricTest";
  private static final Folder<Bubble, List<Bubble>> bubbleFolder = new BubbleFolder<>();

  @Test
  @DisplayName("Test 1:1 Stream zipping")
  void zip() {

    AtomicInteger zippedFooCount = new AtomicInteger(0);
    final Stream<Foo> zippedFoos = FabricTestDataCreator.getZippedFoos();
    zippedFoos.forEach(
        foo -> {
          Log.i(LOG_TAG, "Got zipped Foo: " + foo);
          assertThat(foo).isNotNull();
          zippedFooCount.getAndIncrement();
        });

    final int count = zippedFooCount.get();
    Log.i(LOG_TAG, "Total Zipped Foo count: " + count);
    assertThat(count).isEqualTo(FabricTestDataCreator.END_IDX);
  }

  @Test
  @DisplayName("Test multi-Stream zip")
  void testMultiStreamZip() {

    AtomicInteger zippedMarklarCount = new AtomicInteger(0);
    final Stream<Marklar> zippedMarklars = FabricTestDataCreator.getZippedMarklars();
    zippedMarklars.forEach(
        marklar -> {
          Log.i(LOG_TAG, "Got zipped Marklar:\n" + marklar);
          assertThat(marklar).isNotNull();
          zippedMarklarCount.getAndIncrement();
        });

    final int count = zippedMarklarCount.get();
    Log.i(LOG_TAG, "Zipped Marklar count: " + count);
    assertThat(count).isEqualTo(FabricTestDataCreator.END_IDX);
  }

  @Test
  @DisplayName("Test Stream folding")
  void fold() {

    final Stream<Bubble> bubbleStream = FabricTestDataCreator.getBubbles();

    AtomicInteger foldedBubbleCount = new AtomicInteger(0);
    final Stream<List<Bubble>> foldedBubbles = Fabric.fold(bubbleStream, bubbleFolder);
    foldedBubbles.forEach(
        bubbles -> {
          Log.i(LOG_TAG, "Got folded Bubbles:\n" + bubbles);
          assertThat(bubbles).isNotNull().isNotEmpty();
          assertThat(bubbles.size()).isEqualTo(FabricTestDataCreator.MAX_BUBBLES);
          foldedBubbleCount.getAndIncrement();
        });

    final int count = foldedBubbleCount.get();
    Log.i(LOG_TAG, "Folded Bubble count: " + count);
    assertThat(count).isEqualTo(FabricTestDataCreator.END_IDX);
  }

  @Test
  @DisplayName("Test Stream folding & zipping")
  void testFoldingAndZipping() {

    final Stream<Bubble> bubbleStream = FabricTestDataCreator.getBubbles();
    final Stream<List<Bubble>> foldedBubbles = Fabric.fold(bubbleStream, bubbleFolder);
    final Stream<Marklar> zippedMarklars = FabricTestDataCreator.getZippedMarklars();
    final Stream<Marklar> zippedAndFolded =
        Fabric.zip(zippedMarklars, foldedBubbles, Marklar::addBubbles);

    final AtomicInteger count = new AtomicInteger(0);
    zippedAndFolded.forEach(
        marklar -> {
          Log.i(LOG_TAG, "Zipped & Folded Marklar:\n" + marklar);
          assertThat(marklar).isNotNull();
          assertThat(marklar.getBubbles().size()).isEqualTo(FabricTestDataCreator.MAX_BUBBLES);
          count.incrementAndGet();
        });

    final int actualMarklarCount = count.get();
    Log.i(LOG_TAG, "Final Zipped  Folded item count: " + actualMarklarCount);
    assertThat(actualMarklarCount).isEqualTo(FabricTestDataCreator.END_IDX);
  }
}
