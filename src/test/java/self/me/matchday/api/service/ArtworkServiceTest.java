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
import self.me.matchday.model.Competition;
import self.me.matchday.model.Team;
import self.me.matchday.util.Log;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for artwork service")
class ArtworkServiceTest {

  private static final String LOG_TAG = "ArtworkServiceTest";

  // Test resources
  private static ArtworkService artworkService;
  private static TeamService teamService;
  private static CompetitionService competitionService;
  // Test data
  private static Team testTeam;
  private static Competition testCompetition;
  // Test constants
  private static final int defaultTeamEmblemBytes = 15_182;
  private static final int expectedCompetitionEmblemBytes = 13_989;
  private static final int expectedFanartBytes = 866_211;
  private static final int expectedLandscapeBytes = 189_337;

  @BeforeAll
  static void setUp(
      @Autowired final ArtworkService service,
      @Autowired final TeamService teamService,
      @Autowired final CompetitionService competitionService) {

    ArtworkServiceTest.artworkService = service;
    ArtworkServiceTest.teamService = teamService;
    ArtworkServiceTest.competitionService = competitionService;

    // Create test resources
    final Team team = new Team("Test Team");
    final Competition competition = new Competition("Test Competition");
    // Save to DB
    testTeam = teamService.saveTeam(team);
    testCompetition = competitionService.saveCompetition(competition);

  }

  @AfterAll
  static void tearDown() {
    // delete test resources
    competitionService.deleteCompetitionById(testCompetition.getCompetitionId());
    teamService.deleteTeamById(testTeam.getTeamId());
  }

  @Test
  @DisplayName("Validate default team emblem retrieval")
  void fetchDefaultTeamEmblem() {

    Log.i(LOG_TAG, "Getting emblem artwork for team ID: " + testTeam.getTeamId());
    final Optional<byte[]> teamEmblemOptional =
        artworkService.fetchTeamEmblem(testTeam.getTeamId());
    assertThat(teamEmblemOptional.isPresent()).isTrue();

    final byte[] teamEmblem = teamEmblemOptional.get();
    final int actualByteLength = teamEmblem.length;
    Log.i(LOG_TAG, String.format("Read data, length: %s bytes", actualByteLength));
    assertThat(actualByteLength).isEqualTo(defaultTeamEmblemBytes);
  }

  @Test
  @DisplayName("Validate default team fanart retrieval")
  void fetchDefaultTeamFanart() {

    final Optional<byte[]> optionalBytes = artworkService.fetchTeamFanart(testTeam.getTeamId());
    assertThat(optionalBytes).isPresent();

    optionalBytes.ifPresent(bytes -> {
      Log.i(LOG_TAG, String.format("Read: %s bytes for default team fanart", bytes.length));
      assertThat(bytes.length).isEqualTo(expectedFanartBytes);
    });
  }

  @Test
  @DisplayName("Validate retrieval of default competition emblem")
  void fetchCompetitionEmblem() {

    final Optional<byte[]> optionalBytes =
            artworkService.fetchCompetitionEmblem(testCompetition.getCompetitionId());
    assertThat(optionalBytes).isPresent();

    final int actualCompetitionEmblemBytes = optionalBytes.get().length;
    Log.i(LOG_TAG, String.format("Read: %s bytes for default competition emblem", actualCompetitionEmblemBytes));
    assertThat(actualCompetitionEmblemBytes).isEqualTo(expectedCompetitionEmblemBytes);
  }

  @Test
  @DisplayName("Validation for default competition fanart retrieval")
  void fetchCompetitionFanart() {

    final Optional<byte[]> optionalBytes =
            artworkService.fetchCompetitionFanart(testCompetition.getCompetitionId());
    assertThat(optionalBytes).isPresent();

    final byte[] actualFanartBytes = optionalBytes.get();
    Log.i(LOG_TAG, String.format("Read: %s bytes for default competition fanart", actualFanartBytes.length));
    assertThat(actualFanartBytes.length).isEqualTo(expectedFanartBytes);
  }

  @Test
  @DisplayName("Validate default competition monochrome emblem lookup")
  void fetchCompetitionMonochromeEmblem() {

    final Optional<byte[]> optionalBytes =
            artworkService.fetchCompetitionMonochromeEmblem(testCompetition.getCompetitionId());
    assertThat(optionalBytes).isPresent();

    final int actualMonoEmblemBytes = optionalBytes.get().length;
    Log.i(LOG_TAG, String.format("Read: %s bytes for default competition monochrome emblem", actualMonoEmblemBytes));
    assertThat(actualMonoEmblemBytes).isEqualTo(expectedCompetitionEmblemBytes);
  }

  @Test
  @DisplayName("Validate default competition landscape art retrieval")
  void fetchCompetitionLandscape() {

    final Optional<byte[]> optionalBytes =
            artworkService.fetchCompetitionLandscape(testCompetition.getCompetitionId());
    assertThat(optionalBytes).isPresent();

    final int actualLandscapeBytes = optionalBytes.get().length;
    Log.i(LOG_TAG, String.format("Read: %s bytes for landscape default artwork", actualLandscapeBytes));
    assertThat(actualLandscapeBytes).isEqualTo(expectedLandscapeBytes);
  }
}
