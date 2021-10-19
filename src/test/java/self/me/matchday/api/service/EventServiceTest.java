/*
 * Copyright (c) 2021.
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for Event service")
class EventServiceTest {

  private static final String LOG_TAG = "EventServiceTest";

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

    Log.i(
        LOG_TAG,
        String.format(
            "Saved Event w/ID: %s, Competition ID: %s, Team ID: %s; FileSrcID: %s",
            testMatch.getEventId(), testCompetition, testTeam, testFileSource.getFileSrcId()));
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
    final Optional<List<Event>> eventsOptional = eventService.fetchAllEvents();
    assertThat(eventsOptional).isPresent();

    eventsOptional.ifPresent(
        events -> {
          // Perform tests
          final int actualEventCount = events.size();
          Log.i(
              LOG_TAG,
              String.format(
                  "Testing Event count: expected: %s, actual: %s",
                  expectedEventCount, actualEventCount));
          assertThat(actualEventCount).isGreaterThanOrEqualTo(expectedEventCount);
        });
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
    final String testFileSourceId = testFileSource.getFileSrcId();
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

    // Minimum expected events
    final int expectedEventCount = 1;

    final Optional<List<Event>> optionalEvents =
        eventService.fetchEventsForCompetition(testCompetition.getCompetitionId());

    assertThat(optionalEvents).isPresent();
    optionalEvents.ifPresent(
        events -> assertThat(events.size()).isGreaterThanOrEqualTo(expectedEventCount));
  }

  @Test
  @DisplayName("Ensure fetches all Events for specified Team")
  void fetchEventsForTeam() {

    // Minimum expected Events
    final int expectedEventCount = 1;

    // Fetch Events for Team:
    final Optional<List<Event>> optionalEvents =
        eventService.fetchEventsForTeam(testTeam.getTeamId());
    assertThat(optionalEvents).isPresent();

    optionalEvents.ifPresent(
        events -> assertThat(events.size()).isGreaterThanOrEqualTo(expectedEventCount));
  }

  @Test
  @DisplayName("Ensure an Event can be saved to the database & then deleted")
  void saveAndDeleteEvent() {

    // Create test data
    final Match saveEvent =
        new Match.MatchBuilder()
            .setCompetition(testCompetition)
            .setHomeTeam(testTeam)
            .setAwayTeam(testTeam)
            .setDate(LocalDateTime.now())
            .build();
    saveEvent.addFileSources(List.of(testDataCreator.createTestVideoFileSource()));

    final Optional<List<Event>> optionalEvents = eventService.fetchAllEvents();
    assertThat(optionalEvents).isPresent();
    final List<Event> events = optionalEvents.get();
    final int initialEventCount = events.size();

    // Save test Event to DB
    eventService.saveEvent(saveEvent);
    // Retrieve saved Event from DB
    final Optional<Event> optionalEvent = eventService.fetchById(saveEvent.getEventId());

    // Test retrieved Event
    assertThat(optionalEvent).isPresent();
    optionalEvent.ifPresent(
        event -> {
          Log.i(LOG_TAG, "Retrieved test Event: " + event);
          assertThat(event).isEqualTo(saveEvent);
        });

    // Delete test data
    eventService.deleteEvent(saveEvent);

    // Verify Event count has returned to previous of test
    final Optional<List<Event>> optionalEventsPostTest = eventService.fetchAllEvents();
    assertThat(optionalEventsPostTest).isPresent();
    final List<Event> postTestEvents = optionalEventsPostTest.get();
    final int postTestEventCount = postTestEvents.size();

    assertThat(postTestEventCount).isEqualTo(initialEventCount);
  }
}
