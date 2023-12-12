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
import static self.me.matchday.model.video.Resolution.R_1080p;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.api.service.EventService;
import self.me.matchday.model.*;
import self.me.matchday.model.video.VideoFilePack;
import self.me.matchday.model.video.VideoFileSource;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for Event service")
class EventServiceTest {

  private static final Logger logger = LogManager.getLogger(EventServiceTest.class);
  // Test data
  private static final List<Event> cleanupData = new ArrayList<>();
  // Test resources
  private final TestDataCreator testDataCreator;
  private final EventService eventService;
  private final Match testMatch;
  private final Match unUpdatedEvent;
  private final Competition testCompetition;

  @Autowired
  public EventServiceTest(@NotNull TestDataCreator testDataCreator, EventService eventService) {

    this.testDataCreator = testDataCreator;
    this.eventService = eventService;

    testMatch = testDataCreator.createTestMatch("EventServiceTest");
    cleanupData.add(testMatch);
    testCompetition = testMatch.getCompetition();
    Team testTeam = testMatch.getHomeTeam();

    final Optional<VideoFileSource> fileSourceOptional =
        testMatch.getFileSources().stream().findFirst();
    assertThat(fileSourceOptional).isPresent();
    VideoFileSource testFileSource = fileSourceOptional.get();

    logger.info(
        "Saved Event w/ID: {}, Competition ID: {}, Team ID: {}; FileSrcID: {}",
        testMatch.getEventId(),
        testCompetition.getId(),
        testTeam.getId(),
        testFileSource.getFileSrcId());

    // create Event for updating test
    unUpdatedEvent = testDataCreator.createTestMatch("Non-Updated Event");
    cleanupData.add(unUpdatedEvent);
    logger.info("Created Event for update() test: {}", unUpdatedEvent);
  }

  @AfterAll
  public static void cleanup() throws IOException {
    TestDataCreator.deleteGeneratedMatchArtwork(cleanupData);
  }

  private @NotNull Match createUnsavedMatch(String name) {
    final Match match =
        Match.builder()
            .competition(new Competition(TestDataCreator.getRandomizedName(name, 10, 100)))
            .homeTeam(new Team(TestDataCreator.getRandomizedName(name + " Home", 100, 1000)))
            .awayTeam(new Team(TestDataCreator.getRandomizedName(name + " Away", 100, 1000)))
            .fixture(new Fixture(5))
            .season(new Season())
            .date(LocalDateTime.now())
            .build();
    match.getFileSources().add(createVideoFileSource());
    return match;
  }

  private VideoFileSource createVideoFileSource() {

    final int fileSetCount = 1;
    final List<VideoFilePack> videoFilePacks = testDataCreator.createTestVideoFiles(fileSetCount);
    return VideoFileSource.builder()
        .channel("Event Service Test Channel")
        .resolution(R_1080p)
        .languages("English")
        .videoBitrate(8_000L)
        .videoFilePacks(videoFilePacks)
        .filesize(FileSize.ofGigabytes(8))
        .build();
  }

  @Test
  @DisplayName("Test saving an Event to database")
  void save() {
    final List<Event> initialEvents = eventService.fetchAll();
    final int initialCount = initialEvents.size();
    logger.info("Initial database has: {} Events", initialCount);
    final Event testMatch = createUnsavedMatch("SaveTest " + Math.random());
    logger.info("Created Test Event: {}", testMatch);

    final Event savedEvent = eventService.save(testMatch);
    cleanupData.add(savedEvent);
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
            .mapToObj(i -> createUnsavedMatch("SaveAllMatch " + i))
            .collect(Collectors.toList());
    final List<Event> savedEvents = eventService.saveAll(testEvents);
    cleanupData.addAll(savedEvents);
    logger.info("Saved Event: {}", savedEvents);
    assertThat(savedEvents).isNotNull().isNotEmpty();
    assertThat(savedEvents.size()).isEqualTo(SAVE_COUNT);
  }

  @Test
  @DisplayName("Ensure fetchAllPaged() returns all Events; at least @MIN_EVENT_COUNT")
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
  @DisplayName("Ensure fetches Events for a given Competition")
  void fetchEventsForCompetition() {

    final int page = 0;
    final int pageSize = 16;
    final int minExpectedEventCount = 1;
    final Page<Event> eventsPage =
        eventService.fetchEventsForCompetition(testCompetition.getId(), page, pageSize);
    final List<Event> events = eventsPage.getContent();
    assertThat(events.size()).isGreaterThanOrEqualTo(minExpectedEventCount);
  }

  @Test
  @DisplayName("Validate updating Event in database")
  void update() {

    logger.info("Original Event: {}", unUpdatedEvent);

    final Match testEvent = getPristineEventCopy(unUpdatedEvent);
    final Competition updatedCompetition =
        testDataCreator.createTestCompetition("Updated Competition for EventServiceTest");
    testEvent.setCompetition(updatedCompetition);
    logger.info("Attempting to update Event with: {}", testEvent);

    final Match updatedEvent = (Match) eventService.update(testEvent);
    logger.info("Got updated Event: {}", updatedEvent);
    assertThat(updatedEvent).isNotEqualTo(unUpdatedEvent);
  }

  private @NotNull Match getPristineEventCopy(@NotNull Match event) {

    final Match pristine = new Match();
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
    cleanupData.add(saveEvent);

    final List<Event> initialEvents = eventService.fetchAll();
    final int initialEventCount = initialEvents.size();
    assertThat(initialEventCount).isNotZero();

    // Delete test data
    eventService.delete(saveEvent.getEventId());

    // Verify Event count has returned to previous of test
    final List<Event> postTestEvents = eventService.fetchAll();
    final int postTestEventCount = postTestEvents.size();
    assertThat(postTestEventCount).isEqualTo(initialEventCount - 1);
  }
}
