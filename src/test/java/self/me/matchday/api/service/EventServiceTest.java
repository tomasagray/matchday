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
import self.me.matchday.model.Event;
import self.me.matchday.model.Match;
import self.me.matchday.model.Team;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.util.Log;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for Event service")
class EventServiceTest {

  private static final String LOG_TAG = "EventServiceTest";
  private static final Logger logger = LogManager.getLogger(EventServiceTest.class);

  // Test resources
  private static TestDataCreator testDataCreator;
  private static EventService eventService;

  // Test data
  private static Match testMatch;
  private static VideoFileSource testFileSource;
  private static Competition testCompetition;
  private static Team testTeam;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull final TestDataCreator testDataCreator,
      @Autowired @NotNull final EventService eventService) {

    EventServiceTest.testDataCreator = testDataCreator;
    EventServiceTest.eventService = eventService;

    testMatch = testDataCreator.createTestMatch();
    testCompetition = testMatch.getCompetition();
    testTeam = testMatch.getHomeTeam();

    final Optional<VideoFileSource> fileSourceOptional =
        testMatch.getFileSources().stream().findFirst();
    assertThat(fileSourceOptional).isPresent();
    testFileSource = fileSourceOptional.get();

    logger.info(
        "Saved Event w/ID: {}, Competition ID: {}, Team ID: {}; FileSrcID: {}",
        testMatch.getEventId(),
        testCompetition,
        testTeam,
        testFileSource.getFileSrcId());
  }

  @AfterAll
  static void tearDown() {
    // delete test data
    testDataCreator.deleteTestEvent(testMatch);
  }

  @Test
  @DisplayName("Ensure fetchAllEvents() returns all Events; at least @MIN_EVENT_COUNT")
  void fetchAllEvents() {

    final int expectedEventCount = 1; // minimum
    final List<Event> events = eventService.fetchAllEvents();

    // Perform tests
    final int actualEventCount = events.size();
    Log.i(
        LOG_TAG,
        String.format(
            "Testing Event count: expected: %s, actual: %s", expectedEventCount, actualEventCount));
    assertThat(actualEventCount).isGreaterThanOrEqualTo(expectedEventCount);
  }

  @Test
  @DisplayName("Ensure a specific Event can be recalled from database")
  void fetchById() {

    // Fetch data from database
    final Optional<Event> eventOptional = eventService.fetchById(testMatch.getEventId());
    assertThat(eventOptional).isPresent();

    eventOptional.ifPresent(event -> assertThat(event).isEqualTo(testMatch));
  }

  @Test
  @DisplayName("Ensure a specific video file source can be recalled from database")
  void fetchVideoFileSrc() {

    // Get test event from DB
    final Optional<Event> eventOptional = eventService.fetchById(testMatch.getEventId());
    assertThat(eventOptional).isPresent();
    final Event event = eventOptional.get();
    // Get test file source
    final Optional<VideoFileSource> testFileSrcOptional =
        event.getFileSources().stream().findFirst();
    assertThat(testFileSrcOptional).isPresent();
    // Get file source ID
    final VideoFileSource testFileSource = testFileSrcOptional.get();
    final UUID testFileSourceId = testFileSource.getFileSrcId();
    Log.i(LOG_TAG, "Test VideoFileSource ID: " + testFileSourceId);

    final Optional<VideoFileSource> fileSourceOptional =
        eventService.fetchVideoFileSrc(testFileSourceId);
    assertThat(fileSourceOptional).isPresent();

    fileSourceOptional.ifPresent(
        videoFileSource -> {
          Log.i(LOG_TAG, "Retrieved file source from database: " + videoFileSource);
          assertThat(videoFileSource).isEqualTo(EventServiceTest.testFileSource);
        });
  }

  @Test
  @DisplayName("Ensure fetches Events for a given Competition")
  void fetchEventsForCompetition() {

    final int minExpectedEventCount = 1;
    final List<Event> events =
        eventService.fetchEventsForCompetition(testCompetition.getCompetitionId());
    assertThat(events.size()).isGreaterThanOrEqualTo(minExpectedEventCount);
  }

  @Test
  @DisplayName("Ensure fetches all Events for specified Team")
  void fetchEventsForTeam() {

    // Minimum expected Events
    final int expectedEventCount = 1;

    // Fetch Events for Team:
    final List<Event> events = eventService.fetchEventsForTeam(testTeam.getTeamId());
    assertThat(events.size()).isGreaterThanOrEqualTo(expectedEventCount);
  }

  @Test
  @DisplayName("Ensure an Event can be saved to the database & then deleted")
  void deleteEvent() {

    // Create test data
    final Match saveEvent = testDataCreator.createTestMatch();

    final List<Event> initialEvents = eventService.fetchAllEvents();
    final int initialEventCount = initialEvents.size();
    assertThat(initialEventCount).isNotZero();

    // Delete test data
    eventService.delete(saveEvent);

    // Verify Event count has returned to previous of test
    final List<Event> postTestEvents = eventService.fetchAllEvents();
    final int postTestEventCount = postTestEvents.size();
    assertThat(postTestEventCount).isEqualTo(initialEventCount - 1);
  }
}
