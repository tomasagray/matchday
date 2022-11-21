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

import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.api.service.SynonymService;
import self.me.matchday.db.ProperNameRepository;
import self.me.matchday.model.ProperName;
import self.me.matchday.model.Synonym;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for SynonymService")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class SynonymServiceTest {

  private static final Logger logger = LogManager.getLogger(SynonymServiceTest.class);

  private final SynonymService synonymService;
  private final ProperNameRepository properNameRepository;

  @Autowired
  SynonymServiceTest(SynonymService synonymService, ProperNameRepository properNameRepository) {
    this.synonymService = synonymService;
    this.properNameRepository = properNameRepository;
  }

  @Test
  @DisplayName("Validate a Synonym can be added to & retrieved from database")
  void testAddSynonym() {

    final String synonymName = "Beth";
    final Synonym beth = new Synonym(synonymName);
    final Synonym addedSynonym = synonymService.save(beth);
    logger.info("Added Synonym: " + addedSynonym);
    assertThat(addedSynonym.getName()).isEqualTo(synonymName);
  }

  @Test
  @DisplayName("Validate Synonyms for a ProperName can be retrieved")
  void testSynonymProperNameRetrieval() {

    // given
    final String synonymName = "Sammy";
    final String properNameName = "Samuel";
    final ProperName samuel = new ProperName(properNameName);
    final Synonym sam = new Synonym(synonymName);
    samuel.addSynonym(sam);
    properNameRepository.save(samuel);
    logger.info("Added ProperName: {} with Synonym: {}", samuel, sam);

    // when
    final List<String> synonyms = synonymService.fetchSynonymsFor(properNameName);
    logger.info("Got Synonyms: " + synonyms);

    // then
    assertThat(synonyms.size()).isNotZero();
    synonyms.forEach(
        synonym -> {
          logger.info("Fetching ProperName for: {}", synonym);
          final ProperName properName = synonymService.fetchProperNameFor(synonym);
          logger.info("Found ProperName: {} for Synonym: {}", properName, synonym);
          final List<String> matchingSynonyms =
              properName.getSynonyms().stream().map(Synonym::getName).collect(Collectors.toList());
          matchingSynonyms.add(synonym);
          logger.info("All matching synonyms: {}", matchingSynonyms);
          assertThat(matchingSynonyms).contains(synonym);
        });
  }
}
