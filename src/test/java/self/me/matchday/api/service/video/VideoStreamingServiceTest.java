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
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.UnitTestFileServerPlugin;
import self.me.matchday.api.service.FileServerService;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.Match;
import self.me.matchday.model.video.M3UPlaylist;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for video streaming service")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VideoStreamingServiceTest {

  private static final String LOG_TAG = "VideoStreamingServiceTest";

  private static final long waitSeconds = 60;
  // Service dependencies
  private static TestDataCreator testDataCreator;
  private static VideoStreamingService streamingService;
  private static VideoStreamLocatorPlaylistService locatorPlaylistService;

  // Test data
  private static Match testMatch;
  private static FileServerUser testFileServerUser;
  private static VideoStreamLocatorService streamLocatorService;
  private static EventFile testEventFile;
  private static EventFileSource testFileSource;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull final TestDataCreator testDataCreator,
      @Autowired @NotNull final VideoStreamingService streamingService,
      @Autowired @NotNull final VideoStreamLocatorPlaylistService locatorPlaylistService,
      @Autowired @NotNull final VideoStreamLocatorService streamLocatorService,
      @Autowired @NotNull final FileServerService fileServerService,
      @Autowired @NotNull final UnitTestFileServerPlugin testFileServerPlugin) {

    VideoStreamingServiceTest.testDataCreator = testDataCreator;
    VideoStreamingServiceTest.streamingService = streamingService;
    VideoStreamingServiceTest.streamLocatorService = streamLocatorService;
    VideoStreamingServiceTest.locatorPlaylistService = locatorPlaylistService;

    // Create test user & login
    testFileServerUser = testDataCreator.createTestFileServerUser();
    fileServerService.login(testFileServerUser, testFileServerPlugin.getPluginId());
    assertThat(testFileServerUser.isLoggedIn()).isTrue();

    // Create test data
    VideoStreamingServiceTest.testMatch = testDataCreator.createTestMatch();
    VideoStreamingServiceTest.testFileSource = getTestFileSource();
  }

  @AfterAll
  static void tearDown() {
    Log.i(LOG_TAG, "Deleting test data: " + testMatch);
    // delete test data
    testDataCreator.deleteFileServerUser(testFileServerUser);
    testDataCreator.deleteTestEvent(testMatch);
  }

  @Test
  @Order(1)
  @DisplayName("Validate retrieval of file sources for a given Event")
  void fetchEventFileSources() {

    Log.i(LOG_TAG, "Fetching event with ID: " + testMatch.getEventId());
    final Optional<Collection<EventFileSource>> fileSrcOptional =
        streamingService.fetchEventFileSources(testMatch.getEventId());
    assertThat(fileSrcOptional).isPresent();

    final Collection<EventFileSource> actualFileSources = fileSrcOptional.get();
    Log.i(LOG_TAG, "Retrieved files sources: " + actualFileSources);

    final Set<EventFileSource> expectedFileSources = testMatch.getFileSources();
    assertThat(actualFileSources.size()).isEqualTo(expectedFileSources.size());
    assertThat(actualFileSources).containsAll(expectedFileSources);
  }

  @NotNull
  private static EventFileSource getTestFileSource() {

    AtomicReference<EventFileSource> atomicFileSource = new AtomicReference<>();
    final Set<EventFileSource> fileSources = testMatch.getFileSources();
    fileSources.stream().findFirst().ifPresent(atomicFileSource::set);
    final EventFileSource testFileSource = atomicFileSource.get();

    assertThat(testFileSource).isNotNull();
    return testFileSource;
  }

  @Test
  @Order(2)
  @DisplayName("Test that a playlist is created & returned")
  void getVideoStreamPlaylist() throws Exception {

    // test constants
    final long recheckDelay = 10_000L;
    final int minPlaylistOutputLen = 10;

    // test variables
    final String testEventId = testMatch.getEventId();
    final String testFileSrcId = testFileSource.getEventFileSrcId();
    Log.i(LOG_TAG, "Testing with Match:\n" + testMatch);
    Log.i(
        LOG_TAG,
        String.format(
            "Testing video stream creation with Event ID: %s, File Source ID: %s",
            testEventId, testFileSrcId));

    final Optional<M3UPlaylist> testVideoStreamPlaylist =
        streamingService.getVideoStreamPlaylist(testEventId, testFileSrcId);
    assertThat(testVideoStreamPlaylist).isNotNull().isEmpty();
    Log.i(LOG_TAG, "VideoStreamingService returned Optional.empty(), as expected...");

    Log.i(LOG_TAG, String.format("Waiting %s milliseconds...", recheckDelay));
    Thread.sleep(recheckDelay);
    Log.i(LOG_TAG, "Done waiting, performing recheck...");

    final Optional<M3UPlaylist> afterDelayStreamPlaylist =
        streamingService.getVideoStreamPlaylist(testEventId, testFileSrcId);
    assertThat(afterDelayStreamPlaylist).isNotNull().isPresent();

    final M3UPlaylist testPlaylistOutput = afterDelayStreamPlaylist.get();
    Log.i(LOG_TAG, "Test rendered M3U playlist:\n" + testPlaylistOutput);
    assertThat(testPlaylistOutput).isNotNull().isNotEqualTo("");
    assertThat(testPlaylistOutput.toString().length()).isGreaterThanOrEqualTo(minPlaylistOutputLen);
  }

  @Test
  @Order(3)
  @DisplayName("Validate reading playlist file from disk")
  void readPlaylistFile() {

    final int MIN_PLAYLIST_LEN = 200;

    final Optional<EventFileSource> fileSourceOptional =
        testMatch.getFileSources().stream().findFirst();
    assertThat(fileSourceOptional).isPresent();
    final EventFileSource testFileSource = fileSourceOptional.get();

    // Get test stream locator
    testEventFile = testFileSource.getEventFiles().get(0);
    final Optional<VideoStreamLocator> locatorOptional =
        VideoStreamingServiceTest.streamLocatorService.getStreamLocatorFor(testEventFile);
    assertThat(locatorOptional).isNotNull().isPresent();
    final VideoStreamLocator testStreamLocator = locatorOptional.get();
    Log.i(
        LOG_TAG,
        String.format(
            "Testing playlist file reading with File Source ID: %s, Stream Locator ID: %s",
            testFileSource.getEventFileSrcId(), testStreamLocator.getStreamLocatorId()));

    // Read test playlist file
    final Optional<String> actualPlaylistFile =
        streamingService.readPlaylistFile(
            testMatch.getEventId(),
            testFileSource.getEventFileSrcId(),
            testStreamLocator.getStreamLocatorId());
    Log.i(LOG_TAG, "Read playlist file:\n" + actualPlaylistFile);

    // Perform tests
    assertThat(actualPlaylistFile).isNotNull().isNotEmpty().isPresent();
    final String actualPlaylistData = actualPlaylistFile.get();
    final int actualPlaylistSize = actualPlaylistData.getBytes(StandardCharsets.UTF_8).length;

    Log.i(
        LOG_TAG, String.format("Ensuring read playlist is longer than %d bytes", MIN_PLAYLIST_LEN));
    assertThat(actualPlaylistSize).isGreaterThan(MIN_PLAYLIST_LEN);
  }

  @Test
  @Order(4)
  @DisplayName("Validate reading of video segment (.ts) from disk")
  void getVideoSegmentResource() throws IOException {

    final int minContentLength = 500_000;

    // Get test file source
    final Optional<EventFileSource> fileSourceOptional =
        testMatch.getFileSources().stream().findFirst();
    assertThat(fileSourceOptional).isPresent();
    final EventFileSource testFileSource = fileSourceOptional.get();

    // Get test stream locator
    final Optional<VideoStreamLocator> locatorOptional =
        streamLocatorService.getStreamLocatorFor(testEventFile);
    assertThat(locatorOptional).isPresent();
    final VideoStreamLocator testStreamLocator = locatorOptional.get();
    assertThat(testStreamLocator).isNotNull();
    final Long testStreamLocatorId = testStreamLocator.getStreamLocatorId();
    final String testEventFileSrcId = testFileSource.getEventFileSrcId();
    final String testEventId = testMatch.getEventId();

    Log.i(
        LOG_TAG,
        String.format(
            "Testing video segment reading with Event ID: %s, File Source ID: %s, Stream Locator ID: %s",
            testEventId, testEventFileSrcId, testStreamLocatorId));
    // Read video resource
    final Resource actualVideoResource =
        streamingService.getVideoSegmentResource(
            testEventId, testEventFileSrcId, testStreamLocatorId, "segment_00001");

    Log.d(LOG_TAG, "Read video segment: " + actualVideoResource);
    assertThat(actualVideoResource).isNotNull();
    assertThat(actualVideoResource.contentLength()).isGreaterThan(minContentLength);
  }

  @Test
  @Order(5)
  @DisplayName("Validate ability to delete previously downloaded video data")
  void deleteVideoData() throws IOException, InterruptedException {

    final Optional<VideoStreamLocatorPlaylist> playlistOptional =
        locatorPlaylistService.getVideoStreamPlaylistFor(testFileSource.getEventFileSrcId());
    assertThat(playlistOptional).isPresent();
    final VideoStreamLocatorPlaylist testVideoStreamLocatorPlaylist = playlistOptional.get();
    final List<VideoStreamLocator> streamLocators =
        testVideoStreamLocatorPlaylist.getStreamLocators();

    Log.i(
        LOG_TAG,
        String.format("Waiting %d seconds to ensure streaming processes are dead...", waitSeconds));
    streamingService.killAllStreamingTasks();
    Thread.sleep(waitSeconds * 1_000);

    Log.i(LOG_TAG, "Deleting video data...");
    streamingService.deleteVideoData(testVideoStreamLocatorPlaylist);

    // Validate data has been removed
    streamLocators.forEach(
        streamLocator -> {
          final Path playlistPath = streamLocator.getPlaylistPath();
          final boolean exists = playlistPath.toFile().exists();
          Log.i(LOG_TAG, String.format("Path: %s exists? %s", playlistPath, exists));
          assertThat(exists).isFalse();
        });
  }

  @Test
  @Disabled
  void killAllStreamingTasks() {
    // TODO - write this test
  }
}
