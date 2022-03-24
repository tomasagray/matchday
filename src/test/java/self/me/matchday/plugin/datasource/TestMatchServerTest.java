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

package self.me.matchday.plugin.datasource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.Event;
import self.me.matchday.model.Snapshot;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Exercise test match server")
class TestMatchServerTest {

  private static TestMatchServer plugin;

  @BeforeAll
  public static void setUp(@Autowired TestMatchServer plugin) {
    TestMatchServerTest.plugin = plugin;
  }

  @Test
  @DisplayName("Validate plugin ID")
  void getPluginId() {
    UUID expectedId = UUID.fromString("3acb0e50-9733-44c7-96c2-98908cc508f1");
    UUID actualId = plugin.getPluginId();
    assertThat(actualId).isEqualTo(expectedId);
  }

  @Test
  @DisplayName("Validate plugin title")
  void getTitle() {
    final String expectedTitle = "Test Match Server (localhost)";
    String actualTitle = plugin.getTitle();
    assertThat(actualTitle).isEqualTo(expectedTitle);
  }

  @Test
  @DisplayName("Validate plugin description")
  void getDescription() {
    final String expectedDescription = "Simple match data server for testing";
    String actualDescription = plugin.getDescription();
    assertThat(actualDescription).isEqualTo(expectedDescription);
  }

  @Test
  @DisplayName("Validate Snapshot handling")
  void getSnapshot() throws IOException {

    Snapshot<Event> snapshot = plugin.getAllSnapshots();
    List<Event> testEvents = snapshot.getData().collect(Collectors.toList());
    final int expectedEventCount = 22;
    final int actualEventCount = testEvents.size();
    assertThat(actualEventCount).isGreaterThanOrEqualTo(expectedEventCount);
  }
}
