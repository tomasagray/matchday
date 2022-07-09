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
import self.me.matchday.model.Highlight;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for Highlight Show service")
class HighlightServiceTest {

  private static final Logger logger = LogManager.getLogger(HighlightServiceTest.class);

  private static TestDataCreator testDataCreator;
  private static HighlightService highlightService;
  private static Highlight testHighlight;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull final TestDataCreator testDataCreator,
      @Autowired @NotNull final HighlightService highlightService) {

    HighlightServiceTest.testDataCreator = testDataCreator;
    HighlightServiceTest.highlightService = highlightService;

    // Create test data
    HighlightServiceTest.testHighlight = testDataCreator.createHighlightShow();
  }

  @AfterAll
  static void tearDown() {
    // delete test data from database
    testDataCreator.deleteTestEvent(testHighlight);
  }

  @Test
  @DisplayName("Validate retrieval of all Highlight shows from database")
  void fetchAllHighlights() {

    final int expectedHighlightCount = 1;

    final List<Highlight> highlights = highlightService.fetchAll();
    logger.info("Found Highlight data: {}", highlights);
    assertThat(highlights.size()).isGreaterThanOrEqualTo(expectedHighlightCount);
    assertThat(highlights).contains(testHighlight);
  }

  @Test
  @DisplayName("Validate retrieval of specific Highlight Show")
  void fetchHighlight() {

    final Optional<Highlight> highlightOptional =
        highlightService.fetchById(testHighlight.getEventId());
    assertThat(highlightOptional).isPresent();

    highlightOptional.ifPresent(
        highlight -> {
          logger.info("Retrieved Highlight Show: {}", highlight);
          final boolean equals = highlight.equals(testHighlight);
          assertThat(equals).isTrue();
          assertThat(highlight).isEqualTo(testHighlight);
        });
  }
}
