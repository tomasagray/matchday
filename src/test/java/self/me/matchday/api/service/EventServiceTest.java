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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.*;
import self.me.matchday.util.Log;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static self.me.matchday.model.EventFileSource.Resolution.R_1080p;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for Event service")
class EventServiceTest {

    private static final String LOG_TAG = "EventServiceTest";

    // Test params
    // Test resources
    private static EventService eventService;
    private static Match testMatch;
    private static EventFileSource testFileSource;
    private static Competition testCompetition;
    private static Team testTeam;

    @BeforeAll
    static void setUp(@Autowired final EventService eventService) {

        EventServiceTest.eventService = eventService;

        // Create & save test match & EventFileSource
        testCompetition = new Competition("TEST COMPETITION");
        testTeam = new Team("TEST TEAM");
        testMatch =
                new Match.MatchBuilder()
                        .setDate(LocalDateTime.now())
                        .setCompetition(testCompetition)
                        .setHomeTeam(testTeam)
                        .build();

        testFileSource =
                EventFileSource
                    .builder()
                    .channel("Test Channel")
                    .resolution(R_1080p)
                    .languages(List.of("English"))
                    .build();
        testMatch.getFileSources().add(testFileSource);
//        eventService.saveEvent(testMatch);

//
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }


        Log.i(LOG_TAG,
                String.format("Saved Event w/ID: %s, Competition ID: %s, Team ID: %s; EventFileSrcID: %s",
                        testMatch.getEventId(), testCompetition, testTeam, testFileSource.getEventFileSrcId()));
    }

    @Test
    @DisplayName("Ensure fetchAllEvents() returns all Events; at least @MIN_EVENT_COUNT")
    void fetchAllEvents() {

        final int expectedEventCount = 130;
        final Optional<List<Event>> eventsOptional = eventService.fetchAllEvents();
        assertThat(eventsOptional).isPresent();

        eventsOptional.ifPresent(events -> {
            // Perform tests
            final int actualEventCount = events.size();
            Log.i(LOG_TAG, String.format("Testing Event count: expected: %s, actual: %s", expectedEventCount, actualEventCount));
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
    @DisplayName("Ensure a specific Event file source can be recalled from database")
    void fetchEventFileSrc() {

        Log.i(LOG_TAG, "Test EventFileSource ID: " + testFileSource.getEventFileSrcId());
        // skipped for now
    }

    @Test
    @DisplayName("Ensure fetches Events for a given Competition")
    void fetchEventsForCompetition() {

        // Minimum expected events
        final int expectedEventCount = 5;

        // Fetch Events for La Liga
        final Optional<List<Event>> optionalEvents =
                eventService.fetchEventsForCompetition("ed58c4376cc9990841bc18a81859b545");

        assertThat(optionalEvents).isPresent();

        optionalEvents.ifPresent(events -> assertThat(events.size()).isGreaterThanOrEqualTo(expectedEventCount));
    }

    @Test
    @DisplayName("Ensure fetches all Events for specified Team")
    void fetchEventsForTeam() {

        // Minimum expected Events
        final int expectedEventCount = 1;

        // Fetch Events for Team:
        final Optional<List<Event>> optionalEvents =
                eventService.fetchEventsForTeam("9891739094756d2605946c867b32ad28");
        assertThat(optionalEvents).isPresent();

        optionalEvents.ifPresent(events -> assertThat(events.size()).isGreaterThanOrEqualTo(expectedEventCount));
    }

    @Test
    @DisplayName("Ensure an Event can be saved to the database")
    @Disabled
    void saveEvent() {

        final Match testMatch = new Match();
        testMatch.setEventId("TEST_ID");

        // Save test Event to DB
        eventService.saveEvent(testMatch);
        // Retrieve saved Event from DB
        final Optional<Event> optionalEvent = eventService.fetchById("TEST_ID");
        assertThat(optionalEvent).isPresent();

        optionalEvent.ifPresent(event -> {
            Log.i(LOG_TAG, "Retrieved test Event: " + event);
            assertThat(event).isEqualTo(testMatch);
        });
    }

    @Test
    void deleteEvent() {
        eventService.deleteEvent(testMatch);
    }

  @AfterAll
  static void tearDown() {

      final Optional<Event> optionalEvent = eventService.fetchById(testMatch.getEventId());
      optionalEvent.ifPresent(event -> {
          Log.i(LOG_TAG, "Saved Event is: " + event);
          Log.i(LOG_TAG, "Saved EventFileSource ID is: " + event.getFileSources().get(0).getEventFileSrcId());
      });
      Log.i(LOG_TAG, "Deleting test Event w/ID: " + testMatch.getEventId());
        eventService.deleteEvent(testMatch);
  }

}
