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

package self.me.matchday.api.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.VideoStreamPlaylistLocator;
import self.me.matchday.util.Log;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for playlist locator service")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlaylistLocatorServiceTest {

  private static final String LOG_TAG = "PlaylistLocatorServiceTest";

  private static PlaylistLocatorService playlistLocatorService;
  private static VideoStreamPlaylistLocator testPlaylistLocator;

  @BeforeAll
  static void setUp(@Autowired final PlaylistLocatorService playlistLocatorService) {

    PlaylistLocatorServiceTest.playlistLocatorService = playlistLocatorService;
  }

  @AfterAll
  static void tearDown() {
    // Ensure test data is cleaned up
    playlistLocatorService.deletePlaylistLocator(testPlaylistLocator.getPlaylistId());
  }

  @Test
  @DisplayName("Test retrieval of all playlist locators from database")
  void getAllPlaylistLocators() {

    final int expectedPlaylistLocatorCount = 1;
    final List<VideoStreamPlaylistLocator> playlistLocators =
        playlistLocatorService.getAllPlaylistLocators();
    final int actualPlaylistLocatorCount = playlistLocators.size();
    Log.i(
        LOG_TAG,
        String.format("Fetched %s playlist locators from database", actualPlaylistLocatorCount));

    assertThat(actualPlaylistLocatorCount).isGreaterThanOrEqualTo(expectedPlaylistLocatorCount);
  }

  @Test
  @DisplayName("Test retrieval of specific playlist locator from database")
  void getPlaylistLocator() {}

  @Test
  @DisplayName("Test creation of new playlistlocator")
  @Order(1)
  void createNewPlaylistLocator() {

    testPlaylistLocator =
        playlistLocatorService.createNewPlaylistLocator(
            "TEST_EVENT_ID", UUID.randomUUID(), Path.of("."));

    Log.i(LOG_TAG, "Created playlist locator: " + testPlaylistLocator);
  }

  @Test
  void deletePlaylistLocator() {}
}
