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
import self.me.matchday.model.Team;
import self.me.matchday.model.video.VideoFileSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for Event service")
class EventServiceTest {

  private static final Logger logger = LogManager.getLogger(EventServiceTest.class);

  // Test resources
  private static TestDataCreator testDataCreator;
  private static EventService eventService;

  // Test data
  private static Event testMatch;
  private static VideoFileSource testFileSource;
  private static Competition testCompetition;
  private static Team testTeam;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull final TestDataCreator testDataCreator,
      @Autowired @NotNull final EventService eventService) {

    EventServiceTest.testDataCreator = testDataCreator;
    EventServiceTest.eventService = eventService;

    testMatch = testDataCreator.createTestMatch("EventServiceTest");
    testCompetition = testMatch.getCompetition();
    testTeam = testMatch.getHomeTeam();

    final Optional<VideoFileSource> fileSourceOptional =
        testMatch.getFileSources().stream().findFirst();
    assertThat(fileSourceOptional).isPresent();
    testFileSource = fileSourceOptional.get();

    logger.info(
        "Saved Event w/ID: {}, Competition ID: {}, Team ID: {}; FileSrcID: {}",
        testMatch.getEventId(),
        testCompetition.getCompetitionId(),
        testTeam.getTeamId(),
        testFileSource.getFileSrcId());
  }

  @AfterAll
  static void tearDown() {
    // delete test data
    testDataCreator.deleteTestEvent(testMatch);
  }

  @Test
  @DisplayName("Test saving an Event to database")
  void save() {
    final List<Event> initialEvents = eventService.fetchAll();
    final int initialCount = initialEvents.size();
    logger.info("Initial database has: {} Events", initialCount);
    final Event testMatch = testDataCreator.createTestMatch("SaveTest");
    logger.info("Created Test Event: {}", testMatch);

    final Event savedEvent = eventService.save(testMatch);
    logger.info("Saved Event: {}", savedEvent);
    assertThat(savedEvent).isNotNull();

    final List<Event> afterEvents = eventService.fetchAll();
    final int afterCount = afterEvents.size();
    logger.info("After saving, database contains: {} Events", afterCount);
    final int diff = afterCount - initialCount;
    assertThat(diff).isEqualTo(1);
  }

  @Test
  @DisplayName("Test saving several Events")
  void saveAll() {

    final int SAVE_COUNT = 5;

    final List<Event> initialEvents = eventService.fetchAll();
    final int initialCount = initialEvents.size();
    logger.info("Initial database has: {} Events", initialCount);

    final List<Event> testEvents =
        IntStream.range(0, SAVE_COUNT)
            .mapToObj(i -> testDataCreator.createTestMatch("Match " + i))
            .collect(Collectors.toList());
    final List<Event> savedEvents = eventService.saveAll(testEvents);
    logger.info("Saved Event: {}", savedEvents);
    assertThat(savedEvents).isNotNull().isNotEmpty();

    final List<Event> afterEvents = eventService.fetchAll();
    final int afterCount = afterEvents.size();
    logger.info("After saving, database contains: {} Events", afterCount);
    final int diff = afterCount - initialCount;
    assertThat(diff).isEqualTo(testEvents.size());
  }

  @Test
  @DisplayName("Ensure fetchAll() returns all Events; at least @MIN_EVENT_COUNT")
  void fetchAllEvents() {

    final int expectedEventCount = 1; // minimum
    final List<Event> events = eventService.fetchAll();

    // Perform tests
    final int actualEventCount = events.size();
    logger.info(
        "Testing Event count: expected: {}, actual: {}", expectedEventCount, actualEventCount);
    assertThat(actualEventCount).isGreaterThanOrEqualTo(expectedEventCount);
  }

  @Test
  @DisplayName("Ensure a specific Event can be recalled from database")
  void fetchById() {

    // Fetch data from database
    final Optional<Event> eventOptional = eventService.fetchById(testMatch.getEventId());
    assertThat(eventOptional).isPresent();
    eventOptional.ifPresent(
        event -> {
          // normalize date times
          event.setDate(testMatch.getDate());
          assertThat(event).isEqualTo(testMatch);
        });
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
    logger.info("Test VideoFileSource ID: {}", testFileSourceId);

    final Optional<VideoFileSource> fileSourceOptional =
        eventService.fetchVideoFileSrc(testFileSourceId);
    assertThat(fileSourceOptional).isPresent();

    fileSourceOptional.ifPresent(
        videoFileSource -> {
          logger.info("Retrieved file source from database: {}", videoFileSource);
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
    logger.info("All Matches in database:\n{}", eventService.fetchAll());

    logger.info("Fetching Matches for Team: {}", testTeam);
    final List<Event> events = eventService.fetchEventsForTeam(testTeam.getTeamId());
    logger.info("Got Matches:\n{}", events);
    assertThat(events.size()).isGreaterThanOrEqualTo(expectedEventCount);
  }

  @Test
  @DisplayName("Validate updating Event in database")
  void update() {
    final List<Event> events = eventService.fetchAll();
    if (events.size() == 0) {
      // make sure there is at least one event to test...
      events.add(testDataCreator.createTestMatch("MinimumEvent"));
      eventService.saveAll(events);
    }
    assertThat(events.size()).isGreaterThanOrEqualTo(1);
    final Event originalEvent = events.get(0);
    logger.info("Original Event: {}", originalEvent);

    final Event testEvent = getPristineEventCopy(originalEvent);
    final Competition updatedCompetition = new Competition("Updated Competition");
    updatedCompetition.setCompetitionId(UUID.randomUUID());
    testEvent.setCompetition(updatedCompetition);
    logger.info("Attempting to update Event with: {}", testEvent);

    final Event updatedEvent = eventService.update(testEvent);
    logger.info("Got updated Event: {}", updatedEvent);
    assertThat(updatedEvent).isNotEqualTo(originalEvent);
  }

  private @NotNull Event getPristineEventCopy(@NotNull Event event) {
    final Event pristine = new Event();
    pristine.setEventId(event.getEventId());
    pristine.setCompetition(event.getCompetition());
    pristine.setHomeTeam(event.getHomeTeam());
    pristine.setAwayTeam(event.getAwayTeam());
    pristine.setDate(event.getDate());
    pristine.setFixture(event.getFixture());
    pristine.setSeason(event.getSeason());
    pristine.addAllFileSources(event.getFileSources());
    return pristine;
  }

  @Test
  @DisplayName("Ensure an Event can be saved to the database & then deleted")
  void deleteEvent() {

    // Create test data
    final Event saveEvent = testDataCreator.createTestMatch();

    final List<Event> initialEvents = eventService.fetchAll();
    final int initialEventCount = initialEvents.size();
    assertThat(initialEventCount).isNotZero();

    // Delete test data
    eventService.delete(saveEvent);

    // Verify Event count has returned to previous of test
    final List<Event> postTestEvents = eventService.fetchAll();
    final int postTestEventCount = postTestEvents.size();
    assertThat(postTestEventCount).isEqualTo(initialEventCount - 1);
  }
}
