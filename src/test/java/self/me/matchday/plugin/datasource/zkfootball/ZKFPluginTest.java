/*
 * Copyright (c) 2020.
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

package self.me.matchday.plugin.datasource.zkfootball;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validate ZKF plugin operation")
// todo - blog is private?
@Disabled
class ZKFPluginTest {

  private static final String LOG_TAG = "ZKFPluginTest";

  private static ZKFPlugin zkfPlugin;

  @BeforeAll
  static void setUp(@Autowired final ZKFPlugin plugin) {
    zkfPlugin = plugin;
  }

  @Test
  @DisplayName("Validate snapshot handling")
  void getEventStream() throws IOException {

    // test constants
    final String testTeam = "Napoli";

    // Create test SnapshotRequest
    final SnapshotRequest testSnapshotRequest =
        SnapshotRequest.builder().labels(List.of(testTeam)).build();

    // Get Snapshot of Galataman blog
    final Snapshot<Stream<Event>> actualSnapshot = zkfPlugin.getSnapshot(testSnapshotRequest);
    final Stream<Event> actualSnapshotData = actualSnapshot.getData();

    Log.i(LOG_TAG, "Testing snapshot data...");

    AtomicInteger eventCounter = new AtomicInteger();
    actualSnapshotData.forEach(
        event -> {
          Log.i(LOG_TAG, "Validating Event: " + event);

          // Validate each Event
          assertThat(event.getTitle()).isNotNull().isNotEmpty();
          assertThat(event.getCompetition()).isNotNull();
          assertThat(event.getSeason()).isNotNull();
          assertThat(event.getFixture()).isNotNull();
          eventCounter.getAndIncrement();
        });

    // Validate Event count
    final int actualEventCount = eventCounter.get();
    final int expectedEventCount = 25;

    Log.i(LOG_TAG, String.format("Found %s Events", actualEventCount));
    assertThat(actualEventCount).isEqualTo(expectedEventCount);
  }
}
