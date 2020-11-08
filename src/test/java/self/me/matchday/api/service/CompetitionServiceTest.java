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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.Competition;
import self.me.matchday.util.Log;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for competition service")
class CompetitionServiceTest {

  private static final String LOG_TAG = "CompetitionServiceTest";

  // Test constants
  // Test resources
  private static CompetitionService competitionService;

  @BeforeAll
  static void setUp(@Autowired final CompetitionService service) {
    competitionService = service;
  }

  @Test
  @DisplayName("Verify retrieval of ALL competitions from database")
  void fetchAllCompetitions() {

    final int MIN_REQUIRED_COMPETITIONS = 10;

    final Optional<List<Competition>> competitionsOptional =
        competitionService.fetchAllCompetitions();
    assertThat(competitionsOptional.isPresent()).isTrue();

    final List<Competition> competitions = competitionsOptional.get();
    Log.i(LOG_TAG, String.format("Retrieved %s competitions from database", competitions.size()));
    assertThat(competitions.size()).isGreaterThanOrEqualTo(MIN_REQUIRED_COMPETITIONS);
  }

  @Test
  @DisplayName("Validate retrieval of a single competition from database")
  void fetchCompetitionById() {

    final String TEST_COMPETITION_ID = "ed58c4376cc9990841bc18a81859b545";

    final Optional<Competition> competitionOptional =
        competitionService.fetchCompetitionById(TEST_COMPETITION_ID);
    assertThat(competitionOptional.isPresent()).isTrue();

    final Competition expectedCompetition = new Competition("La Liga");
    final Competition actualCompetition = competitionOptional.get();
    Log.i(
        LOG_TAG,
        String.format(
            "Using ID: %s, retrieved competition from database:\n%s",
            TEST_COMPETITION_ID, actualCompetition));

    assertThat(actualCompetition).isEqualTo(expectedCompetition);

  }
}