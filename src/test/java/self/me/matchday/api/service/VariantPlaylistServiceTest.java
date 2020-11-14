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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.CreateTestData;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.Match;
import self.me.matchday.model.VariantM3U;
import self.me.matchday.plugin.fileserver.FileServerPlugin;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.util.Log;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for variant playlist service")
class VariantPlaylistServiceTest {

  private static final String LOG_TAG = "VariantPlaylistServiceTest";

  private static VariantPlaylistService playlistService;
  private static EventService eventService;
  private static FileServerService fileServerService;
  private static CompetitionService competitionService;
  private static TeamService teamService;

  private static Match testMatch;
  private static FileServerUser testFileServerUser;

  @BeforeAll
  static void setUp(
      @Autowired final VariantPlaylistService playlistService,
      @Autowired final EventService eventService,
      @Autowired final FileServerService fileServerService,
      @Autowired final CompetitionService competitionService,
      @Autowired final TeamService teamService) {

    VariantPlaylistServiceTest.playlistService = playlistService;
    VariantPlaylistServiceTest.eventService = eventService;
    VariantPlaylistServiceTest.fileServerService = fileServerService;
    VariantPlaylistServiceTest.competitionService = competitionService;
    VariantPlaylistServiceTest.teamService = teamService;

    // Register test file server plugin & user
    final FileServerPlugin testFileServerPlugin = CreateTestData.createTestFileServerPlugin();
    testFileServerUser = CreateTestData.createTestFileServerUser();
    fileServerService.getFileServerPlugins().add(testFileServerPlugin);
    fileServerService.login(testFileServerUser, testFileServerPlugin.getPluginId());

    // Create & save test data
    Match match = CreateTestData.createTestMatch();
    eventService.saveEvent(match);

    // Retrieve managed copy
    final Optional<Event> eventOptional = eventService.fetchById(match.getEventId());
    assertThat(eventOptional).isPresent();
    testMatch = (Match) eventOptional.get();
  }

  @AfterAll
  static void tearDown() {

    Log.i(LOG_TAG, "Deleting test data...");
    // delete test data
    eventService.deleteEvent(testMatch);
    competitionService.deleteCompetitionById(testMatch.getCompetition().getCompetitionId());
    teamService.deleteTeamById(testMatch.getHomeTeam().getTeamId());
    fileServerService.deleteUser(testFileServerUser.getUserId());
    fileServerService.getFileServerPlugins().clear();
  }

  @Test
  @DisplayName("Validate creation of variant M3U playlist")
  void fetchVariantPlaylist() {

    // Get test file source
    final Optional<EventFileSource> fileSourceOptional = testMatch.getFileSources().stream().findFirst();
    assertThat(fileSourceOptional).isPresent();

    final EventFileSource testFileSource = fileSourceOptional.get();
    assertThat(testFileSource).isNotNull();

    // Run test
    final Optional<VariantM3U> playlistOptional =
        playlistService.fetchVariantPlaylist(testFileSource.getEventFileSrcId());
    assertThat(playlistOptional).isPresent();

    final VariantM3U actualPlaylist = playlistOptional.get();
    Log.i(LOG_TAG, "Created Variant M3U playlist:\n" + actualPlaylist);
    assertThat(actualPlaylist).isNotNull();
  }
}
