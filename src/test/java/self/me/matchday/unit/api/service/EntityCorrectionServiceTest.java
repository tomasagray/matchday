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

package self.me.matchday.unit.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.api.service.EntityCorrectionService;
import self.me.matchday.api.service.EventService;
import self.me.matchday.api.service.SynonymService;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.Match;
import self.me.matchday.model.ProperName;
import self.me.matchday.model.Season;
import self.me.matchday.model.Synonym;
import self.me.matchday.model.Team;
import self.me.matchday.model.video.PartIdentifier;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFilePack;
import self.me.matchday.model.video.VideoFileSource;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing & validation for the Entity correction service")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class EntityCorrectionServiceTest {

  static final String UEFA_CHAMPIONS_LEAGUE = "UEFA Champions League ";
  private static final Logger logger = LogManager.getLogger(EntityCorrectionServiceTest.class);
  private static final String FC_BARCELONA = "FC Barcelona ";
  private static final String ATLETICO_DE_MADRID = "Atletico de Madrid ";
  // Test data
  private static final List<Event> cleanupData = new ArrayList<>();
  private final EntityCorrectionService entityCorrectionService;
  private final EventService eventService;
  private final SynonymService synonymService;

  @Autowired
  public EntityCorrectionServiceTest(
      EntityCorrectionService correctionService,
      @NotNull EventService eventService,
      SynonymService synonymService)
      throws MalformedURLException {
    this.entityCorrectionService = correctionService;
    this.eventService = eventService;
    this.synonymService = synonymService;

    createProperEvent();
    createSynonyms();
  }

  @AfterAll
  static void cleanup() throws IOException {
    TestDataCreator.deleteGeneratedMatchArtwork(cleanupData);
  }

  private void createProperEvent() throws MalformedURLException {
    final Match properEvent =
        Match.builder()
            .competition(new Competition(UEFA_CHAMPIONS_LEAGUE))
            .homeTeam(new Team(FC_BARCELONA))
            .awayTeam(new Team(ATLETICO_DE_MADRID))
            .date(LocalDate.now().atStartOfDay())
            .build();
    final VideoFileSource fileSource = new VideoFileSource();
    final VideoFilePack filePack = new VideoFilePack();
    filePack.put(new VideoFile(PartIdentifier.FIRST_HALF, new URL("https://wwww.testing.com/1")));
    filePack.put(new VideoFile(PartIdentifier.SECOND_HALF, new URL("https://wwww.testing.com/2")));
    fileSource.addVideoFilePack(filePack);
    properEvent.addFileSource(fileSource);
    final Event saved = eventService.save(properEvent);
    cleanupData.add(saved);
    logger.info("Saved proper event: " + saved);
  }

  private void createSynonyms() {
    final ProperName fcBarcelona = new ProperName(FC_BARCELONA);
    createSynonym("Barca", fcBarcelona);
    final ProperName atletico = new ProperName(ATLETICO_DE_MADRID);
    createSynonym("Atleti", atletico);
    final ProperName championsLeague = new ProperName(UEFA_CHAMPIONS_LEAGUE);
    createSynonym("UCL", championsLeague);
  }

  private void createSynonym(@NotNull String name, ProperName properName) {

    final List<String> synonyms = synonymService.fetchSynonymsFor(name);
    if (synonyms.size() == 0) {
      properName.addSynonym(new Synonym(name));
      logger.info("Added ProperName with Synonym: {}", synonymService.addProperName(properName));
    } else {
      logger.info("Synonym already exists for name: {}", name);
    }
  }

  @Test
  @DisplayName("Test correcting an Event")
  void testEventEntityCorrection() throws ReflectiveOperationException {

    final Match testEvent =
        Match.builder()
            .competition(new Competition("UCL"))
            .homeTeam(new Team("Barca"))
            .awayTeam(new Team("Atleti"))
            .date(LocalDateTime.now())
            .build();
    logger.info("Created uncorrected Event: " + testEvent);

    entityCorrectionService.correctEntityFields(testEvent);
    logger.info("Got corrected Event: " + testEvent);
    assertThat(testEvent.getCompetition().getName().getName()).isEqualTo(UEFA_CHAMPIONS_LEAGUE);
    assertThat(testEvent.getHomeTeam().getName().getName()).isEqualTo(FC_BARCELONA);
    assertThat(testEvent.getAwayTeam().getName().getName()).isEqualTo(ATLETICO_DE_MADRID);
  }

  @Test
  @DisplayName("Ensure fields not marked for correction are not altered")
  void testNonCorrectedFields() throws ReflectiveOperationException {

    final Season testSeason = new Season(2022, 2023);
    final Fixture testFixture = new Fixture(16);
    final LocalDateTime testDate = LocalDateTime.now();
    final Match testEvent =
        Match.builder()
            .competition((new Competition("UCL")))
            .homeTeam(new Team("Atleti"))
            .awayTeam(new Team("Barca"))
            .season(testSeason)
            .fixture(testFixture)
            .date(testDate)
            .build();
    logger.info("Created raw event: " + testEvent);

    entityCorrectionService.correctEntityFields(testEvent);

    logger.info("Event has been corrected to: " + testEvent);
    assertThat(testEvent.getCompetition().getName().getName()).isEqualTo(UEFA_CHAMPIONS_LEAGUE);
    assertThat(testEvent.getHomeTeam().getName().getName()).isEqualTo(ATLETICO_DE_MADRID);
    assertThat(testEvent.getAwayTeam().getName().getName()).isEqualTo(FC_BARCELONA);
    assertThat(testEvent.getSeason()).isEqualTo(testSeason);
    assertThat(testEvent.getFixture()).isEqualTo(testFixture);
    assertThat(testEvent.getDate()).isEqualTo(testDate);
  }
}
