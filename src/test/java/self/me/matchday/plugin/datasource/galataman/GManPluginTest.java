/*
 * Copyright (c) 2021.
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

package self.me.matchday.plugin.datasource.galataman;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.Event;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validate Galataman plugin operation")
class GManPluginTest {

  private static final String LOG_TAG = "GManPluginTest";

  private static GManPlugin gManPlugin;

  @BeforeAll
  static void setUp(@Autowired final GManPlugin plugin) {
    gManPlugin = plugin;
  }

  @Test
  @DisplayName("Validate snapshot handling")
  void testSnapshot() throws IOException {

    // test constants
    final int MIN_FILE_SOURCE_COUNT = 1;

    // Create test SnapshotRequest
    final SnapshotRequest testSnapshotRequest = SnapshotRequest.builder().build();

    // Get Snapshot of Galataman blog
    final Snapshot<Stream<Event>> actualSnapshot = gManPlugin.getSnapshot(testSnapshotRequest);
    final Stream<Event> actualSnapshotData = actualSnapshot.getData();

    Log.i(LOG_TAG, "Testing snapshot data...");

    AtomicInteger eventCounter = new AtomicInteger();
    actualSnapshotData.forEach(
        event -> {
          final String eventTitle = event.getTitle();
          Log.i(LOG_TAG, "Validating Event title: " + eventTitle);

          // Validate each Event
          assertThat(eventTitle).isNotNull().isNotEmpty();

          assertThat(event.getCompetition()).isNotNull();
          assertThat(event.getSeason()).isNotNull();
          assertThat(event.getFixture()).isNotNull();
          assertThat(event.getFileSources().size()).isGreaterThanOrEqualTo(MIN_FILE_SOURCE_COUNT);

          eventCounter.getAndIncrement();
        });

    // Validate Event count
    final int actualEventCount = eventCounter.get();
    final int expectedEventCount = 3;

    Log.i(LOG_TAG, String.format("Found %s Events", actualEventCount));
    assertThat(actualEventCount).isGreaterThanOrEqualTo(expectedEventCount);
  }
}
