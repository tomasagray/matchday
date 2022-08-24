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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.model.ProperName;
import self.me.matchday.model.Synonym;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("Test adding & retrieving words to/from Synonym database")
@Transactional
class SynonymRepositoryTest {

  private static final Logger logger = LogManager.getLogger(SynonymRepositoryTest.class);
  private final SynonymRepository repository;

  @Autowired
  SynonymRepositoryTest(SynonymRepository repository) {
    this.repository = repository;
  }

  @Test
  @DisplayName("Test adding Synonyms, read back")
  void testSynonymRepo() {

    final String testName = "Elizabeth";
    final ProperName elizabeth = new ProperName(testName);
    final Synonym beth = new Synonym("Beth", elizabeth);
    final Synonym lizzie = new Synonym("Lizzie", elizabeth);
    final Synonym betty = new Synonym("Betty", elizabeth);

    logger.info(
        "Saving ProperName: {}, with Synonyms:\n {}, {}, {}", elizabeth, beth, lizzie, betty);

    repository.saveAllAndFlush(List.of(beth, lizzie, betty));

    logger.info("Fetching Synonyms for: {}...", testName);
    final List<Synonym> synonyms = repository.findSynonymsFor(testName);
    logger.info("Got Synonyms: {}", synonyms);
    assertThat(synonyms).isNotNull().isNotEmpty();
    assertThat(synonyms.size()).isEqualTo(3);

    final Optional<Synonym> one = repository.findOne(Example.of(beth));
    assertThat(one).isPresent();
    final Synonym synonym = one.get();
    logger.info("Got Synonym for Elizabeth: " + synonym);
  }

  @Test
  @DisplayName("Get Synonym for a word")
  void testGetSynonymForWord() {

    final String expectedProperName = "perambulate";
    final String synonymWord = "walk";

    final ProperName perambulate = new ProperName(expectedProperName);
    final Synonym walk = new Synonym(synonymWord, perambulate);
    repository.save(walk);

    final Optional<Synonym> optional = repository.findSynonymByName(synonymWord);
    assertThat(optional).isPresent();
    final Synonym synonym = optional.get();
    logger.info("Got Synonym: " + synonym);

    assertThat(synonym.getProperName().getName()).isEqualTo(expectedProperName);
  }
}
