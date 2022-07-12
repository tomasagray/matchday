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

package self.me.matchday.api.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Team;

import java.io.IOException;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for artwork service")
class ArtworkServiceTest {

  private static final Logger logger = LogManager.getLogger(ArtworkServiceTest.class);

  // Test constants
  private static final int defaultTeamEmblemBytes = 15_182;
  private static final int expectedCompetitionEmblemBytes = 13_989;
  private static final int expectedFanartBytes = 866_211;
  private static final int expectedLandscapeBytes = 189_337;

  // Test resources
  private static ArtworkService artworkService;
  private static TestDataCreator testDataCreator;

  // Test data
  private static Competition testCompetition;
  private static Team testTeam;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull TestDataCreator testDataCreator,
      @Autowired @NotNull ArtworkService service) {

    ArtworkServiceTest.artworkService = service;
    ArtworkServiceTest.testDataCreator = testDataCreator;
    ArtworkServiceTest.testCompetition =
        testDataCreator.createTestCompetition("Artwork_Competition_" + new Random().nextInt());
    ArtworkServiceTest.testTeam = testDataCreator.createTestTeam();
  }

  @AfterAll
  static void tearDown() {
    testDataCreator.deleteTestCompetition(testCompetition);
    testDataCreator.deleteTestTeam(testTeam);
  }

  @Test
  @DisplayName("Validate default team emblem retrieval")
  void fetchDefaultTeamEmblem() throws IOException {

    final UUID teamId = testTeam.getTeamId();
    logger.info("Getting emblem artwork for team ID: " + teamId);
    final Optional<byte[]> teamEmblemOptional = artworkService.fetchTeamEmblem(teamId);
    assertThat(teamEmblemOptional.isPresent()).isTrue();

    final byte[] teamEmblem = teamEmblemOptional.get();
    final int actualByteLength = teamEmblem.length;
    logger.info("Read data, length: {} bytes", actualByteLength);
    assertThat(actualByteLength).isEqualTo(defaultTeamEmblemBytes);
  }

  @Test
  @DisplayName("Validate default team fanart retrieval")
  void fetchDefaultTeamFanart() throws IOException {

    final Optional<byte[]> optionalBytes = artworkService.fetchTeamFanart(testTeam.getTeamId());
    assertThat(optionalBytes).isPresent();

    optionalBytes.ifPresent(
        bytes -> {
          logger.info(String.format("Read: %s bytes for default team fanart", bytes.length));
          assertThat(bytes.length).isEqualTo(expectedFanartBytes);
        });
  }

  @Test
  @DisplayName("Validate retrieval of default competition emblem")
  void fetchCompetitionEmblem() throws IOException {

    final Optional<byte[]> optionalBytes =
        artworkService.fetchCompetitionEmblem(testCompetition.getCompetitionId());
    assertThat(optionalBytes).isPresent();

    final int actualCompetitionEmblemBytes = optionalBytes.get().length;
    logger.info("Read: {} bytes for default competition emblem", actualCompetitionEmblemBytes);
    assertThat(actualCompetitionEmblemBytes).isEqualTo(expectedCompetitionEmblemBytes);
  }

  @Test
  @DisplayName("Validation for default competition fanart retrieval")
  void fetchCompetitionFanart() throws IOException {

    final Optional<byte[]> optionalBytes =
        artworkService.fetchCompetitionFanart(testCompetition.getCompetitionId());
    assertThat(optionalBytes).isPresent();

    final byte[] actualFanartBytes = optionalBytes.get();
    logger.info("Read: {} bytes for default competition fanart", actualFanartBytes.length);
    assertThat(actualFanartBytes.length).isEqualTo(expectedFanartBytes);
  }

  @Test
  @DisplayName("Validate default competition monochrome emblem lookup")
  void fetchCompetitionMonochromeEmblem() throws IOException {

    final Optional<byte[]> optionalBytes =
        artworkService.fetchCompetitionMonochromeEmblem(testCompetition.getCompetitionId());
    assertThat(optionalBytes).isPresent();

    final int actualMonoEmblemBytes = optionalBytes.get().length;
    logger.info("Read: {} bytes for default competition monochrome emblem", actualMonoEmblemBytes);
    assertThat(actualMonoEmblemBytes).isEqualTo(expectedCompetitionEmblemBytes);
  }

  @Test
  @DisplayName("Validate default competition landscape art retrieval")
  void fetchCompetitionLandscape() throws IOException {

    final Optional<byte[]> optionalBytes =
        artworkService.fetchCompetitionLandscape(testCompetition.getCompetitionId());
    assertThat(optionalBytes).isPresent();

    final int actualLandscapeBytes = optionalBytes.get().length;
    logger.info("Read: {} bytes for landscape default artwork", actualLandscapeBytes);
    assertThat(actualLandscapeBytes).isEqualTo(expectedLandscapeBytes);
  }
}
