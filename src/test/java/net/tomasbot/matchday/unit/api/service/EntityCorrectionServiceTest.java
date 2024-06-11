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

package net.tomasbot.matchday.unit.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import net.tomasbot.matchday.TestDataCreator;
import net.tomasbot.matchday.api.service.EntityCorrectionService;
import net.tomasbot.matchday.api.service.EventService;
import net.tomasbot.matchday.model.*;
import net.tomasbot.matchday.model.video.PartIdentifier;
import net.tomasbot.matchday.model.video.VideoFile;
import net.tomasbot.matchday.model.video.VideoFilePack;
import net.tomasbot.matchday.model.video.VideoFileSource;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing & validation for the Entity correction service")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class EntityCorrectionServiceTest {

  // constants
  static final String UEFA_CHAMPIONS_LEAGUE = "UEFA Champions League ";
  private static final Random R = new Random();
  private static final Logger logger = LogManager.getLogger(EntityCorrectionServiceTest.class);
  private static final String FC_BARCELONA = "FC Barcelona ";
  private static final String ATLETICO_DE_MADRID = "Atletico de Madrid ";
  // Test data
  private static final List<Event> cleanupData = new ArrayList<>();
  private final EntityCorrectionService entityCorrectionService;
  private final EventService eventService;
  private final int seed = R.nextInt(100);
  private final Competition competition = new Competition(UEFA_CHAMPIONS_LEAGUE + seed);
  private final Competition searchCompetition = new Competition("UCL" + seed);
  private final Synonym competitionSynonym = new Synonym("UCL" + seed);
  private final Team homeTeam = new Team(FC_BARCELONA + seed);
  private final Team searchHomeTeam = new Team("Barca" + seed);
  private final Synonym homeTeamSynonym = new Synonym("Barca" + seed);
  private final Team awayTeam = new Team(ATLETICO_DE_MADRID + seed);
  private final Team searchAwayTeam = new Team("Atleti" + seed);
  private final Synonym awayTeamSynonym = new Synonym("Atleti" + seed);

  @Autowired
  public EntityCorrectionServiceTest(
      EntityCorrectionService correctionService, @NotNull EventService eventService) {
    this.entityCorrectionService = correctionService;
    this.eventService = eventService;
  }

  @AfterAll
  static void cleanup() throws IOException {
    TestDataCreator.deleteGeneratedMatchArtwork(cleanupData);
  }

  @BeforeEach
  void createProperEvent() throws MalformedURLException {
    final Match properEvent =
        Match.builder()
            .competition(competition)
            .homeTeam(homeTeam)
            .awayTeam(awayTeam)
            .date(LocalDate.now().atStartOfDay())
            .build();
    final VideoFileSource fileSource = new VideoFileSource();
    final VideoFilePack filePack = new VideoFilePack();
    filePack.put(new VideoFile(PartIdentifier.FIRST_HALF, new URL("https://wwww.testing.com/1")));
    filePack.put(new VideoFile(PartIdentifier.SECOND_HALF, new URL("https://wwww.testing.com/2")));
    fileSource.addVideoFilePack(filePack);
    properEvent.addFileSource(fileSource);
    createSynonyms(properEvent);
    final Event saved = eventService.save(properEvent);
    logger.info("Saved proper event: {}", saved);
    cleanupData.add(saved);
  }

  void createSynonyms(@NotNull Match event) {
    event.getCompetition().getName().addSynonym(competitionSynonym);
    event.getHomeTeam().getName().addSynonym(homeTeamSynonym);
    event.getAwayTeam().getName().addSynonym(awayTeamSynonym);
  }

  @Test
  @DisplayName("Test correcting an Event")
  void testEventEntityCorrection() throws ReflectiveOperationException {
    final Match testEvent =
        Match.builder()
            .competition(searchCompetition)
            .homeTeam(searchHomeTeam)
            .awayTeam(searchAwayTeam)
            .date(LocalDateTime.now())
            .build();
    logger.info("Created uncorrected Event: {}", testEvent);

    entityCorrectionService.correctEntityFields(testEvent);
    logger.info("Got corrected Event: {}", testEvent);
    assertThat(testEvent.getCompetition().getName().getName())
        .isEqualTo(UEFA_CHAMPIONS_LEAGUE + seed);
    assertThat(testEvent.getHomeTeam().getName().getName()).isEqualTo(FC_BARCELONA + seed);
    assertThat(testEvent.getAwayTeam().getName().getName()).isEqualTo(ATLETICO_DE_MADRID + seed);
  }

  @Test
  @DisplayName("Ensure fields not marked for correction are not altered")
  void testNonCorrectedFields() throws ReflectiveOperationException {
    final Season testSeason = new Season(2022, 2023);
    final Fixture testFixture = new Fixture(16);
    final LocalDateTime testDate = LocalDateTime.now();

    final Match testEvent =
        Match.builder()
            .competition(searchCompetition)
            .homeTeam(searchHomeTeam)
            .awayTeam(searchAwayTeam)
            .season(testSeason)
            .fixture(testFixture)
            .date(testDate)
            .build();
    logger.info("Created raw event: {}", testEvent);

    entityCorrectionService.correctEntityFields(testEvent);

    logger.info("Event has been corrected to: {}", testEvent);
    assertThat(testEvent.getCompetition().getName().getName())
        .isEqualTo(UEFA_CHAMPIONS_LEAGUE + seed);
    assertThat(testEvent.getHomeTeam().getName().getName()).isEqualTo(FC_BARCELONA + seed);
    assertThat(testEvent.getAwayTeam().getName().getName()).isEqualTo(ATLETICO_DE_MADRID + seed);
    assertThat(testEvent.getSeason()).isEqualTo(testSeason);
    assertThat(testEvent.getFixture()).isEqualTo(testFixture);
    assertThat(testEvent.getDate()).isEqualTo(testDate);
  }
}
