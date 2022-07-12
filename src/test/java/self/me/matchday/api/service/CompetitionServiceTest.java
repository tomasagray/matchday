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

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for competition service")
class CompetitionServiceTest {

  private static final Logger logger = LogManager.getLogger(CompetitionServiceTest.class);

  // Test resources
  private static CompetitionService competitionService;
  private static TestDataCreator testDataCreator;
  private static Competition testCompetition;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull CompetitionService service,
      @Autowired @NotNull TestDataCreator testDataCreator) {

    final Random random = new Random();
    CompetitionServiceTest.competitionService = service;
    CompetitionServiceTest.testDataCreator = testDataCreator;
    CompetitionServiceTest.testCompetition =
        testDataCreator.createTestCompetition("CST_Competition_" + random.nextInt());
  }

  @AfterAll
  static void tearDown() {
    // delete test data from DB
    testDataCreator.deleteTestCompetition(testCompetition);
  }

  @Test
  @DisplayName("Verify retrieval of ALL competitions from database")
  void fetchAllCompetitions() {

    final int MIN_REQUIRED_COMPETITIONS = 1;

    final List<Competition> competitions = competitionService.fetchAll();
    logger.info("Retrieved {} competitions from database", competitions.size());
    assertThat(competitions.size()).isGreaterThanOrEqualTo(MIN_REQUIRED_COMPETITIONS);
  }

  @Test
  @DisplayName("Validate retrieval of a single competition from database")
  void fetchCompetitionById() {

    final UUID testCompetitionId = testCompetition.getCompetitionId();

    final Optional<Competition> competitionOptional =
        competitionService.fetchById(testCompetitionId);
    assertThat(competitionOptional.isPresent()).isTrue();

    final Competition actualCompetition = competitionOptional.get();
    logger.info(
        "Using ID: {}, retrieved competition from database:\n{}",
        testCompetitionId,
        actualCompetition);

    assertThat(actualCompetition).isEqualTo(testCompetition);
  }
}
