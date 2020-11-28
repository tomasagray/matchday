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
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.CreateTestData;
import self.me.matchday.TestFileServerPlugin;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.Match;
import self.me.matchday.model.VideoStreamPlaylistLocator;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for video streaming service")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VideoStreamingServiceTest {

  private static final String LOG_TAG = "VideoStreamingServiceTest";

  // Service dependencies
  private static VideoStreamingService streamingService;
  private static EventService eventService;
  private static CompetitionService competitionService;
  private static TeamService teamService;
  private static FileServerService fileServerService;

  // Test data
  private static Match testMatch;
  private static VideoStreamPlaylistLocator testPlaylistLocator;
  private static FileServerUser testFileServerUser;

  @BeforeAll
  static void setUp(
          @Autowired final VideoStreamingService streamingService,
          @Autowired final EventService eventService,
          @Autowired final CompetitionService competitionService,
          @Autowired final TeamService teamService,
          @Autowired final FileServerService fileServerService,
          @Autowired final TestFileServerPlugin testFileServerPlugin) {

    VideoStreamingServiceTest.streamingService = streamingService;
    VideoStreamingServiceTest.eventService = eventService;
    VideoStreamingServiceTest.competitionService = competitionService;
    VideoStreamingServiceTest.teamService = teamService;
    VideoStreamingServiceTest.fileServerService = fileServerService;

    // Create test file server plugin & register

    // Create test user & login
    testFileServerUser = CreateTestData.createTestFileServerUser();
    fileServerService.login(testFileServerUser, testFileServerPlugin.getPluginId());
    assertThat(testFileServerUser.isLoggedIn()).isTrue();

    // Create test data
    final Match match = CreateTestData.createTestMatch();
    // Save to DB
    eventService.saveEvent(match);

    // Get managed copy
    final Optional<Event> eventOptional = eventService.fetchById(match.getEventId());
    assertThat(eventOptional).isPresent();
    VideoStreamingServiceTest.testMatch = (Match) eventOptional.get();
  }

  @AfterAll
  static void tearDown() {

    Log.i(LOG_TAG, "Deleting test data: " + testMatch);
    // delete test data
    eventService.deleteEvent(testMatch);
    competitionService.deleteCompetitionById(testMatch.getCompetition().getCompetitionId());
    teamService.deleteTeamById(testMatch.getHomeTeam().getTeamId());

    fileServerService.deleteUser(testFileServerUser.getUserId());
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

  @Test
  @Order(2)
  @DisplayName("Validate creation of video stream resources")
  void createVideoStream() throws IOException, InterruptedException {

    // Get test file source
    final Optional<EventFileSource> fileSourceOptional =
        testMatch.getFileSources().stream().findFirst();
    assertThat(fileSourceOptional).isPresent();
    final EventFileSource testFileSource = fileSourceOptional.get();
    Log.i(
        LOG_TAG, String.format("Using test event: %s\nFile source:%s", testMatch, testFileSource));

    final Optional<VideoStreamPlaylistLocator> playlistLocatorOptional =
        streamingService.createVideoStream(
            testMatch.getEventId(), testFileSource.getEventFileSrcId());
    assertThat(playlistLocatorOptional).isPresent();

    testPlaylistLocator = playlistLocatorOptional.get();
    Log.i(LOG_TAG, "Test created playlist locator: " + testPlaylistLocator);
    assertThat(testPlaylistLocator.getPlaylistId()).isNotNull();
    assertThat(testPlaylistLocator.getPlaylistPath()).isNotNull();

    // Give FFMPEG a chance to create test data
    final int waitSeconds = 5;
    Log.i(LOG_TAG, String.format("Waiting for %s seconds...", waitSeconds));
    Thread.sleep(waitSeconds * 1000);
  }

  @Test
  @Order(3)
  @DisplayName("Validate reading playlist file from disk")
  void readPlaylistFile() {

    final Optional<EventFileSource> fileSourceOptional =
        testMatch.getFileSources().stream().findFirst();
    assertThat(fileSourceOptional).isPresent();

    final EventFileSource testFileSource = fileSourceOptional.get();

    // Read test playlist file
    final String actualPlaylistFile =
        streamingService.readPlaylistFile(
            testMatch.getEventId(), testFileSource.getEventFileSrcId());
    Log.i(LOG_TAG, "Read playlist file:\n" + actualPlaylistFile);

    assertThat(actualPlaylistFile).isNotNull().isNotEmpty();
  }

  @Test
  @Order(4)
  @DisplayName("Validate reading of video segment (.ts) from disk")
  void getVideoSegmentResource() throws IOException {

    final Optional<EventFileSource> fileSourceOptional =
        testMatch.getFileSources().stream().findFirst();
    assertThat(fileSourceOptional).isPresent();
    final EventFileSource testFileSource = fileSourceOptional.get();
    final Resource actualVideoResource =
        streamingService.getVideoSegmentResource(
            testMatch.getEventId(), testFileSource.getEventFileSrcId(), "segment_00001");

    Log.d(LOG_TAG, "Read video segment: " + actualVideoResource);
    assertThat(actualVideoResource).isNotNull();
    assertThat(actualVideoResource.contentLength()).isGreaterThan(1_000_000);
  }

  @Test
  @Order(5)
  @DisplayName("Validate ability to delete previously downloaded video data")
  void deleteVideoData() throws IOException {

    // Kill streaming
    streamingService.killAllStreamingTasks();

    final Path playlistPath = testPlaylistLocator.getPlaylistPath();

    // File count before deleting
    final int preDeleteFileCount = playlistPath.getParent().toFile().list().length;
    Log.i(
        LOG_TAG,
        String.format("Before deleting there are: %s files in directory", preDeleteFileCount));
    assertThat(preDeleteFileCount).isGreaterThan(0);

    streamingService.deleteVideoData(testPlaylistLocator);

    // File count after deleting
    final File postDeleteParentDir = playlistPath.getParent().toFile();
    assertThat(postDeleteParentDir).doesNotExist();
  }
}
