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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.UnitTestFileServerPlugin;
import self.me.matchday.api.service.FileServerService;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.Match;
import self.me.matchday.model.video.M3UPlaylist;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.util.Log;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for variant playlist service")
class VariantPlaylistServiceTest {

  private static final String LOG_TAG = "VariantPlaylistServiceTest";

  private static TestDataCreator testDataCreator;
  private static VariantPlaylistService playlistService;

  private static Match testMatch;
  private static FileServerUser testFileServerUser;

  @BeforeAll
  static void setUp(
      @Autowired final TestDataCreator testDataCreator,
      @Autowired final VariantPlaylistService playlistService,
      @Autowired final FileServerService fileServerService,
      @Autowired final UnitTestFileServerPlugin testFileServerPlugin) {

    VariantPlaylistServiceTest.testDataCreator = testDataCreator;
    VariantPlaylistServiceTest.playlistService = playlistService;

    // Register test file server plugin
    // Create test user & login
    testFileServerUser = testDataCreator.createTestFileServerUser();
    fileServerService.login(testFileServerUser, testFileServerPlugin.getPluginId());

    // Create & save test data
    VariantPlaylistServiceTest.testMatch = testDataCreator.createTestMatch();
  }

  @AfterAll
  static void tearDown() {
    Log.i(LOG_TAG, "Deleting test data...");
    // delete test data
    testDataCreator.deleteTestEvent(testMatch);
    testDataCreator.deleteFileServerUser(testFileServerUser);
  }

  @Test
  @DisplayName("Validate creation of variant M3U playlist")
  void fetchVariantPlaylist() {

    // Get test file source
    final Optional<EventFileSource> fileSourceOptional =
        testMatch.getFileSources().stream().findFirst();
    assertThat(fileSourceOptional).isPresent();

    final EventFileSource testFileSource = fileSourceOptional.get();
    assertThat(testFileSource).isNotNull();

    // Run test
    final Optional<M3UPlaylist> playlistOptional =
        playlistService.fetchVariantPlaylist(testFileSource.getEventFileSrcId());
    assertThat(playlistOptional).isPresent();

    final M3UPlaylist actualPlaylist = playlistOptional.get();
    Log.i(LOG_TAG, "Created Variant M3U playlist:\n" + actualPlaylist);
    assertThat(actualPlaylist).isNotNull();
  }
}
