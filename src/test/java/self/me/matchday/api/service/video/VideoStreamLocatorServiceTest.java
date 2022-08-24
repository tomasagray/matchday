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

package self.me.matchday.api.service.video;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.video.PartIdentifier;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoStreamLocator;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("Testing for playlist locator service")
class VideoStreamLocatorServiceTest {

  private static final Logger logger = LogManager.getLogger(VideoStreamLocatorServiceTest.class);

  private static final Path storageLocation = Path.of("C:\\Users\\Public\\Matchday\\videos\\_test");
  private final VideoStreamLocatorService videoStreamLocatorService;
  private final VideoFile testVideoFile;
  private final Path testStorage;
  // test resources
  private VideoStreamLocator testStreamLocator;

  @Autowired
  public VideoStreamLocatorServiceTest(
      @NotNull TestDataCreator testDataCreator,
      VideoStreamLocatorService videoStreamLocatorService) {

    this.videoStreamLocatorService = videoStreamLocatorService;
    // Get managed copy of test file source
    VideoFileSource testVideoFileSource = testDataCreator.createVideoFileSourceAndSave();
    this.testVideoFile =
        testVideoFileSource.getVideoFilePacks().get(0).get(PartIdentifier.FIRST_HALF);
    // resolve test data storage path
    this.testStorage = storageLocation.resolve(testVideoFileSource.getFileSrcId().toString());
  }

  @AfterEach
  void tearDown() {
    // Ensure test data is cleaned up
    if (testStreamLocator != null) {
      logger.info("Deleting test locator from DB...: " + testStreamLocator.getStreamLocatorId());
      videoStreamLocatorService.deleteStreamLocator(testStreamLocator);
    }
  }

  @Test
  @DisplayName("Test creation of new playlist locator")
  void createNewPlaylistLocator() {

    assertThat(testVideoFile).isNotNull();
    testStreamLocator = videoStreamLocatorService.createStreamLocator(testStorage, testVideoFile);

    logger.info("Created playlist locator: " + testStreamLocator);
    assertThat(testStreamLocator).isNotNull();
    assertThat(testStreamLocator.getStreamLocatorId()).isNotNull();
  }

  @Test
  @DisplayName("Test retrieval of all playlist locators from database")
  void getAllPlaylistLocators() {

    final int expectedPlaylistLocatorCount = 1;
    logger.info("Adding test stream locator to database...");
    testStreamLocator = videoStreamLocatorService.createStreamLocator(testStorage, testVideoFile);

    final List<VideoStreamLocator> playlistLocators =
        videoStreamLocatorService.getAllStreamLocators();
    final int actualPlaylistLocatorCount = playlistLocators.size();
    logger.info("Fetched {} playlist locators from database", actualPlaylistLocatorCount);

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
    logger.info("Retrieved playlist locator: " + actualStreamLocator);

    // Test playlist locator fields; timestamp will be different
    assertThat(actualStreamLocator.getStreamLocatorId())
        .isEqualTo(testStreamLocator.getStreamLocatorId());
    assertThat(actualStreamLocator.getPlaylistPath())
        .isEqualTo(testStreamLocator.getPlaylistPath());
    assertThat(actualStreamLocator.getVideoFile()).isEqualTo(testStreamLocator.getVideoFile());
    assertThat(actualStreamLocator.getState()).isEqualTo(testStreamLocator.getState());
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
