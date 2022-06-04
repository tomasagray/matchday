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

package self.me.matchday.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.*;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("Test adding & retrieving Events with the EventRepository")
class EventRepositoryTest {

  private static final Logger logger = LogManager.getLogger(EventRepositoryTest.class);

  private static EventRepository eventRepository;

  @BeforeAll
  static void setup(@Autowired EventRepository eventRepository) {
    EventRepositoryTest.eventRepository = eventRepository;
  }

  @Test
  @DisplayName("Fetch Event by matching characteristics")
  void testMatchingEventRetrieval() {

    final Event testEvent =
        Match.builder()
            .competition(new Competition("UEFA Champions League"))
            .homeTeam(new Team("Chelsea"))
            .awayTeam(new Team("AC Milan"))
            .season(new Season(2024, 2025))
            .fixture(new Fixture(34))
            .date(LocalDate.now().atStartOfDay())
            .build();
    logger.info("Saved Event: {}", eventRepository.saveAndFlush(testEvent));

    final Event example =
        Match.builder()
            .competition(new Competition("UEFA Champions League"))
            .homeTeam(new Team("Chelsea"))
            .awayTeam(new Team("AC Milan"))
            .date(LocalDate.now().atStartOfDay())
            .build();
    final Optional<Event> eventOptional = eventRepository.findOne(Example.of(example));
    assertThat(eventOptional).isPresent();
    final Event retrievedEvent = eventOptional.get();
    logger.info("Retrieved Event: {}", retrievedEvent);
    assertThat(retrievedEvent).isNotNull();
  }
}
