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
import self.me.matchday.model.MasterM3U;
import self.me.matchday.model.Match;
import self.me.matchday.util.Log;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for master playlist service")
class MasterPlaylistServiceTest {

  private static final String LOG_TAG = "MasterPlaylistServiceTest";

  private static MasterPlaylistService playlistService;
  private static EventService eventService;
  private static CompetitionService competitionService;
  private static TeamService teamService;

  private static Match testMatch;

  @BeforeAll
  static void setUp(
          @Autowired final MasterPlaylistService playlistService,
          @Autowired final EventService eventService,
          @Autowired final CompetitionService competitionService,
          @Autowired final TeamService teamService) {

    MasterPlaylistServiceTest.playlistService = playlistService;
    MasterPlaylistServiceTest.eventService = eventService;
    MasterPlaylistServiceTest.competitionService = competitionService;
    MasterPlaylistServiceTest.teamService = teamService;

    // Create test data
    Match match = CreateTestData.createTestMatch();
    // Save test data to DB
    eventService.saveEvent(match);

    // Get managed copy for testing
    final Optional<Event> testEventOptional = eventService.fetchById(match.getEventId());
    assertThat(testEventOptional).isPresent();

    testMatch = (Match) testEventOptional.get();
  }

  @AfterAll
  static void tearDown() {

    // delete test data
    eventService.deleteEvent(testMatch);
    competitionService.deleteCompetitionById(testMatch.getCompetition().getCompetitionId());
    teamService.deleteTeamById(testMatch.getHomeTeam().getTeamId());
  }

  @Test
  @DisplayName("Validate generation of master playlist for specified Event")
  void fetchMasterPlaylistForEvent() {

    final Optional<MasterM3U> playlistOptional =
        playlistService.fetchMasterPlaylistForEvent(testMatch.getEventId());
    assertThat(playlistOptional).isPresent();

    playlistOptional.ifPresent(masterM3U -> {
      Log.i(LOG_TAG, "Generated master playlist: " + masterM3U);
      assertThat(masterM3U).isNotNull();
      assertThat(masterM3U.toString()).isNotEmpty();
    });
  }
}
