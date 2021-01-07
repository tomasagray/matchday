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
import self.me.matchday.model.Highlight;
import self.me.matchday.util.Log;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for Highlight Show service")
class HighlightServiceTest {

  private static final String LOG_TAG = "HighlightServiceTest";

  private static HighlightService highlightService;
  private static Highlight testHighlight;
  private static EventService eventService;
  private static CompetitionService competitionService;

  @BeforeAll
  static void setUp(
      @Autowired final HighlightService highlightService,
      @Autowired final EventService eventService,
      @Autowired final CompetitionService competitionService) {

    HighlightServiceTest.highlightService = highlightService;
    HighlightServiceTest.eventService = eventService;
    HighlightServiceTest.competitionService = competitionService;

    // Create test data
    HighlightServiceTest.testHighlight = CreateTestData.createHighlightShow();

    // Add test highlight show to DB
    HighlightServiceTest.eventService.saveEvent(testHighlight);
  }

  @AfterAll
  static void tearDown() {

    // delete test data from database
    eventService.deleteEvent(testHighlight);
    competitionService.deleteCompetitionById(testHighlight.getCompetition().getCompetitionId());
  }

  @Test
  @DisplayName("Validate retrieval of all Highlight shows from database")
  void fetchAllHighlights() {

    final int expectedHighlightCount = 1;

    final Optional<List<Highlight>> highlightsOptional = highlightService.fetchAllHighlights();
    assertThat(highlightsOptional).isPresent();

    highlightsOptional.ifPresent(
        highlights -> {
          Log.i(LOG_TAG, "Found Highlight data: " + highlights);
          assertThat(highlights.size()).isGreaterThanOrEqualTo(expectedHighlightCount);
          assertThat(highlights).contains(testHighlight);
        });
  }

  @Test
  @DisplayName("Validate retrieval of specific Highlight Show")
  void fetchHighlight() {

    final Optional<Highlight> highlightOptional =
        highlightService.fetchHighlight(testHighlight.getEventId());
    assertThat(highlightOptional).isPresent();

    highlightOptional.ifPresent(
        highlight -> {
          Log.i(LOG_TAG, "Retrieved Highlight Show: " + highlight);
          final boolean equals = highlight.equals(testHighlight);
          assertThat(equals).isTrue();
          assertThat(highlight).isEqualTo(testHighlight);
        });
  }
}
