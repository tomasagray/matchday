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

package self.me.matchday.unit.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.db.EventRepository;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.Match;
import self.me.matchday.model.Season;
import self.me.matchday.model.Team;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("Test adding & retrieving Events with the EventRepository")
class EventRepositoryTest {

  private static final Logger logger = LogManager.getLogger(EventRepositoryTest.class);

  private final EventRepository eventRepository;

  @Autowired
  EventRepositoryTest(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  @Test
  @DisplayName("Fetch Event by matching characteristics")
  void testMatchingEventRetrieval() {
    final int seed = ThreadLocalRandom.current().nextInt();
    final String competitionName = "UEFA Champions League " + seed;
    final String homeTeamName = "Chelsea " + seed;
    final String awayTeamName = "AC Milan " + seed;
    final Event testEvent =
        Match.builder()
            .competition(new Competition(competitionName))
            .homeTeam(new Team(homeTeamName))
            .awayTeam(new Team(awayTeamName))
            .season(new Season(2024, 2025))
            .fixture(new Fixture(34))
            .date(LocalDate.now().atStartOfDay())
            .build();
    logger.info("Saved Event: {}", eventRepository.saveAndFlush(testEvent));

    final Event example =
        Match.builder()
            .competition(new Competition(competitionName))
            .homeTeam(new Team(homeTeamName))
            .awayTeam(new Team(awayTeamName))
            .date(LocalDate.now().atStartOfDay())
            .build();
    final Optional<Event> eventOptional = eventRepository.findOne(Example.of(example));
    assertThat(eventOptional).isPresent();
    final Event retrievedEvent = eventOptional.get();
    logger.info("Retrieved Event: {}", retrievedEvent);
    assertThat(retrievedEvent).isNotNull();
  }
}
