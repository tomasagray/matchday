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

package self.me.matchday.api.service.video;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.video.PartIdentifier;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.util.Log;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for playlist locator service")
class VideoStreamLocatorServiceTest {

  private static final String LOG_TAG = "PlaylistLocatorServiceTest";

  private static TestDataCreator testDataCreator;
  private static VideoStreamLocatorService videoStreamLocatorService;

  // test resources
  private static VideoStreamLocator testStreamLocator;
  private static VideoFileSource testVideoFileSource;
  private static VideoFile testVideoFile;

  private static final Path storageLocation = Path.of("C:\\Users\\Public\\Matchday\\videos\\_test");
  private static Path testStorage;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull final TestDataCreator testDataCreator,
      @Autowired @NotNull final VideoStreamLocatorService videoStreamLocatorService) {

    VideoStreamLocatorServiceTest.testDataCreator = testDataCreator;
    VideoStreamLocatorServiceTest.videoStreamLocatorService = videoStreamLocatorService;
    // Get managed copy of test file source
    VideoStreamLocatorServiceTest.testVideoFileSource = testDataCreator.createTestVideoFileSource();
    VideoStreamLocatorServiceTest.testVideoFile =
        VideoStreamLocatorServiceTest.testVideoFileSource
            .getVideoFilePacks()
            .get(0)
            .get(PartIdentifier.FIRST_HALF);
    // resolve test data storage path
    VideoStreamLocatorServiceTest.testStorage =
        storageLocation.resolve(testVideoFileSource.getFileSrcId());
  }

  @AfterAll
  static void tearDownDependencies() {
    // Cleanup test resources
    testDataCreator.deleteVideoFileSource(testVideoFileSource);
    videoStreamLocatorService.deleteStreamLocator(testStreamLocator);
  }

  @AfterEach
  void tearDown() {

    // Ensure test data is cleaned up
    if (testStreamLocator != null) {
      Log.i(LOG_TAG, "Deleting test locator from DB...: " + testStreamLocator.getStreamLocatorId());
      videoStreamLocatorService.deleteStreamLocator(testStreamLocator);
    }
  }

  @Test
  @DisplayName("Test creation of new playlist locator")
  void createNewPlaylistLocator() {

    assertThat(testVideoFile).isNotNull();
    testStreamLocator = videoStreamLocatorService.createStreamLocator(testStorage, testVideoFile);

    Log.i(LOG_TAG, "Created playlist locator: " + testStreamLocator);
    assertThat(testStreamLocator).isNotNull();
    assertThat(testStreamLocator.getStreamLocatorId()).isNotNull();
  }

  @Test
  @DisplayName("Test retrieval of all playlist locators from database")
  void getAllPlaylistLocators() {

    final int expectedPlaylistLocatorCount = 1;
    Log.i(LOG_TAG, "Adding test stream locator to database...");
    testStreamLocator = videoStreamLocatorService.createStreamLocator(testStorage, testVideoFile);

    final List<VideoStreamLocator> playlistLocators =
        videoStreamLocatorService.getAllStreamLocators();
    final int actualPlaylistLocatorCount = playlistLocators.size();
    Log.i(
        LOG_TAG,
        String.format("Fetched %s playlist locators from database", actualPlaylistLocatorCount));

    assertThat(actualPlaylistLocatorCount).isGreaterThanOrEqualTo(expectedPlaylistLocatorCount);
    // Remove test resource
    videoStreamLocatorService.deleteStreamLocator(testStreamLocator);
  }

  @Test
  @DisplayName("Test retrieval of specific playlist locator from database")
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void getPlaylistLocator() {

    // Create test resource
    testStreamLocator = videoStreamLocatorService.createStreamLocator(testStorage, testVideoFile);

    final Long testStreamLocatorId = testStreamLocator.getStreamLocatorId();
    final Optional<VideoStreamLocator> streamLocatorOptional =
        videoStreamLocatorService.getStreamLocator(testStreamLocatorId);
    assertThat(streamLocatorOptional).isPresent();

    final VideoStreamLocator actualStreamLocator = streamLocatorOptional.get();
    Log.i(LOG_TAG, "Retrieved playlist locator: " + actualStreamLocator);

    // Test playlist locator fields; timestamp will be different
    assertThat(actualStreamLocator).isEqualTo(testStreamLocator);
  }

  @Test
  @DisplayName("Test deletion of playlist locator")
  void deletePlaylistLocator() {

    // Create test resource
    testStreamLocator = videoStreamLocatorService.createStreamLocator(testStorage, testVideoFile);

    final int sizeBeforeDelete = videoStreamLocatorService.getAllStreamLocators().size();
    // Perform deletion
    videoStreamLocatorService.deleteStreamLocator(testStreamLocator);
    final int sizeAfterDelete = videoStreamLocatorService.getAllStreamLocators().size();
    final int actualDifference = sizeBeforeDelete - sizeAfterDelete;
    final int expectedDifference = 1;
    assertThat(actualDifference).isEqualTo(expectedDifference);
  }
}
