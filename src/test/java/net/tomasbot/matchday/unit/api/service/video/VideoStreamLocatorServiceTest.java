/*
 * Copyright (c) 2023.
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

package net.tomasbot.matchday.unit.api.service.video;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.tomasbot.matchday.TestDataCreator;
import net.tomasbot.matchday.api.service.video.VideoStreamLocatorService;
import net.tomasbot.matchday.model.video.PartIdentifier;
import net.tomasbot.matchday.model.video.VideoFile;
import net.tomasbot.matchday.model.video.VideoFileSource;
import net.tomasbot.matchday.model.video.VideoStreamLocator;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for playlist locator service")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class VideoStreamLocatorServiceTest {

  private static final Logger logger = LogManager.getLogger(VideoStreamLocatorServiceTest.class);

  private final VideoStreamLocatorService videoStreamLocatorService;
  private final VideoFile testVideoFile;
  private final VideoFileSource testVideoFileSource;
  // test resources
  private Path testStorage;
  private VideoStreamLocator testStreamLocator;

  @Value("${DATA_ROOT}/videos")
  private Path storageLocation;

  @Autowired
  public VideoStreamLocatorServiceTest(
      @NotNull TestDataCreator testDataCreator,
      VideoStreamLocatorService videoStreamLocatorService) {
    this.videoStreamLocatorService = videoStreamLocatorService;
    // Get managed copy of test file source
    this.testVideoFileSource = testDataCreator.createVideoFileSourceAndSave();
    this.testVideoFile =
        testVideoFileSource.getVideoFilePacks().get(0).get(PartIdentifier.FIRST_HALF);
  }

  private Path getTestStorage() {
    if (this.testStorage == null) {
      UUID fileSrcId = testVideoFileSource.getFileSrcId();
      this.testStorage = storageLocation.resolve(fileSrcId.toString());
    }
    return this.testStorage;
  }

  @AfterEach
  void tearDown() {
    // Ensure test data is cleaned up
    if (testStreamLocator != null) {
      Long locatorId = testStreamLocator.getStreamLocatorId();
      videoStreamLocatorService
          .getStreamLocator(locatorId)
          .ifPresent(
              locator -> {
                logger.info("Deleting test locator from DB...: {}", locatorId);
                videoStreamLocatorService.deleteStreamLocator(testStreamLocator);
              });
    }
  }

  @Test
  @DisplayName("Test creation of new playlist locator")
  void createNewPlaylistLocator() {
    assertThat(testVideoFile).isNotNull();
    testStreamLocator =
        videoStreamLocatorService.createStreamLocator(getTestStorage(), testVideoFile);

    logger.info("Created playlist locator: {}", testStreamLocator);
    assertThat(testStreamLocator).isNotNull();
    assertThat(testStreamLocator.getStreamLocatorId()).isNotNull();
  }

  @Test
  @DisplayName("Test retrieval of all playlist locators from database")
  void getAllPlaylistLocators() {
    final int expectedPlaylistLocatorCount = 1;
    logger.info("Adding test stream locator to database...");
    testStreamLocator =
        videoStreamLocatorService.createStreamLocator(getTestStorage(), testVideoFile);

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
    testStreamLocator =
        videoStreamLocatorService.createStreamLocator(getTestStorage(), testVideoFile);

    final Long testStreamLocatorId = testStreamLocator.getStreamLocatorId();
    final Optional<VideoStreamLocator> streamLocatorOptional =
        videoStreamLocatorService.getStreamLocator(testStreamLocatorId);
    assertThat(streamLocatorOptional).isPresent();

    final VideoStreamLocator actualStreamLocator = streamLocatorOptional.get();
    logger.info("Retrieved playlist locator: {}", actualStreamLocator);

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
    testStreamLocator =
        videoStreamLocatorService.createStreamLocator(getTestStorage(), testVideoFile);
    final int sizeBeforeDelete = videoStreamLocatorService.getAllStreamLocators().size();
    assertThat(sizeBeforeDelete).isGreaterThan(0);

    // Perform deletion
    videoStreamLocatorService.deleteStreamLocator(testStreamLocator);
    final int sizeAfterDelete = videoStreamLocatorService.getAllStreamLocators().size();
    final int actualDifference = sizeBeforeDelete - sizeAfterDelete;
    final int expectedDifference = 1;
    assertThat(actualDifference).isEqualTo(expectedDifference);
  }
}
