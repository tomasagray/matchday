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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.CreateTestData;
import self.me.matchday.db.EventFileSrcRepository;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.VideoStreamLocator;
import self.me.matchday.util.Log;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for playlist locator service")
class PlaylistLocatorServiceTest {

  private static final String LOG_TAG = "PlaylistLocatorServiceTest";

  private static PlaylistLocatorService playlistLocatorService;
  private static EventFileSrcRepository fileSrcRepository;
  private static VideoStreamLocator testStreamLocator;
  private static EventFileSource testEventFileSource;
  private static EventFile testEventFile;

  @BeforeAll
  static void setUp(
      @Autowired final PlaylistLocatorService playlistLocatorService,
      @Autowired final EventFileSrcRepository fileSrcRepository) {

    PlaylistLocatorServiceTest.playlistLocatorService = playlistLocatorService;
    PlaylistLocatorServiceTest.fileSrcRepository = fileSrcRepository;
    // Get managed copy of test file source
    final EventFileSource fileSource = CreateTestData.createTestEventFileSource();
    PlaylistLocatorServiceTest.testEventFileSource = fileSrcRepository.save(fileSource);

    PlaylistLocatorServiceTest.testEventFile =
        PlaylistLocatorServiceTest.testEventFileSource.getEventFiles().get(0);
  }

  @AfterAll
  static void tearDownDependencies() {

    // Cleanup test resources
    fileSrcRepository.delete(testEventFileSource);
  }

  @AfterEach
  void tearDown() {

    // Ensure test data is cleaned up
    if (testStreamLocator != null) {
      Log.i(LOG_TAG, "Deleting test locator from DB...: " + testStreamLocator.getStreamLocatorId());
      playlistLocatorService.deletePlaylistLocator(testStreamLocator);
    }
  }

  @Test
  @DisplayName("Test creation of new playlist locator")
  void createNewPlaylistLocator() {

    assertThat(testEventFile).isNotNull();
    testStreamLocator =
        playlistLocatorService.createNewPlaylistLocator(testEventFileSource, testEventFile);

    Log.i(LOG_TAG, "Created playlist locator: " + testStreamLocator);
    assertThat(testStreamLocator).isNotNull();
    assertThat(testStreamLocator.getStreamLocatorId()).isNotNull();
  }

  @Test
  @DisplayName("Test retrieval of all playlist locators from database")
  void getAllPlaylistLocators() {

    final int expectedPlaylistLocatorCount = 1;
    final List<VideoStreamLocator> playlistLocators =
        playlistLocatorService.getAllPlaylistLocators();
    final int actualPlaylistLocatorCount = playlistLocators.size();
    Log.i(
        LOG_TAG,
        String.format("Fetched %s playlist locators from database", actualPlaylistLocatorCount));

    assertThat(actualPlaylistLocatorCount).isGreaterThanOrEqualTo(expectedPlaylistLocatorCount);
  }

  @Test
  @DisplayName("Test retrieval of specific playlist locator from database")
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void getPlaylistLocator() {

    // Create test resource
    testStreamLocator =
        playlistLocatorService.createNewPlaylistLocator(testEventFileSource, testEventFile);

    final Long testStreamLocatorId = testStreamLocator.getStreamLocatorId();
    final Optional<VideoStreamLocator> playlistLocatorOptional =
        playlistLocatorService.getStreamLocator(testStreamLocatorId);
    assertThat(playlistLocatorOptional).isPresent();

    final VideoStreamLocator actualPlaylistLocator = playlistLocatorOptional.get();
    Log.i(LOG_TAG, "Retrieved playlist locator: " + actualPlaylistLocator);

    // Test playlist locator fields; timestamp will be different
    // todo - should playlist timestamps be compared for equality?
    assertThat(actualPlaylistLocator.getStreamLocatorId())
        .isEqualTo(testStreamLocator.getStreamLocatorId());
    assertThat(actualPlaylistLocator.getPlaylistPath())
        .isEqualTo(testStreamLocator.getPlaylistPath());
    assertThat(actualPlaylistLocator.getEventFile()).isEqualTo(testStreamLocator.getEventFile());
  }

  @Test
  @DisplayName("Test deletion of playlist locator")
  void deletePlaylistLocator() {

    // Create test resource
    testStreamLocator =
        playlistLocatorService.createNewPlaylistLocator(testEventFileSource, testEventFile);

    final int sizeBeforeDelete = playlistLocatorService.getAllPlaylistLocators().size();
    // Perform deletion
    playlistLocatorService.deletePlaylistLocator(testStreamLocator);
    final int sizeAfterDelete = playlistLocatorService.getAllPlaylistLocators().size();
    final int actualDifference = sizeBeforeDelete - sizeAfterDelete;
    final int expectedDifference = 1;
    assertThat(actualDifference).isEqualTo(expectedDifference);
  }
}
