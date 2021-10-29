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

import org.hibernate.Hibernate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.Corrected;
import self.me.matchday.CorrectedOrNull;
import self.me.matchday.model.*;
import self.me.matchday.util.Log;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing & validation for the Entity correction service")
class EntityCorrectionServiceTest {

  private static final String LOG_TAG = "EntityCorrectionServiceTest";

  private static EntityCorrectionService correctionService;

  @BeforeAll
  static void setup(@Autowired EntityCorrectionService correctionService) {
    EntityCorrectionServiceTest.correctionService = correctionService;
  }

  @Contract(pure = true)
  @NotNull
  @Unmodifiable
  private static List<String> getTestSynonymList() {
    return List.of("testing", "checking", "probing");
  }

  @Test
  @DisplayName("Test retrieval of synonyms for a given word")
  void getSynonymsFor() {

    final String testWord = "testing";
    final List<String> words = getTestSynonymList();

    System.out.println("Adding synonyms to database: " + words);
    final Synonym addedSynonym = correctionService.createSynonymsFrom(words);
    System.out.println("Added: " + addedSynonym);

    System.out.println("Attempting to retrieve by keyword: " + testWord);
    final Synonym retrievedSynonym = correctionService.getSynonymsFor(testWord);
    System.out.println("Got Synonym from database:" + retrievedSynonym);
    assertThat(retrievedSynonym).isNotNull().isEqualTo(addedSynonym);
  }

  @Test
  @DisplayName("Test recursive entity correction")
  void correctFields() throws ReflectiveOperationException {

    // Create test data
    final List<String> testWords = getTestSynonymList();
    Log.i(LOG_TAG, "Adding synonym words: " + testWords);
    // Save to DB
    final Synonym synonym = correctionService.createSynonymsFrom(testWords);
    Log.i(LOG_TAG, "Added Synonym to database: " + synonym);

    final TestSubCorrectionEntity testSubCorrectionEntity1 =
        new TestSubCorrectionEntity("checking", "no synonyms");
    final TestCorrectionEntity testCorrectionEntity =
        new TestCorrectionEntity("probing", testSubCorrectionEntity1);

    Log.i(LOG_TAG, "Entity PRIOR to correction:\n" + testCorrectionEntity);
    // Correct data
    correctionService.correctFields(testCorrectionEntity);
    Log.i(LOG_TAG, "Entity AFTER correction:\n" + testCorrectionEntity);

    // Run test
    final String correctedStringField = testCorrectionEntity.getCorrectedStringField();
    final TestSubCorrectionEntity correctedSubEntity = testCorrectionEntity.getSubEntity();
    assertThat(correctedSubEntity).isNotNull();
    final String correctedSubStringField = correctedSubEntity.getCorrectedStringField();
    final String uncorrectedStringField = correctedSubEntity.getUncorrectedString();

    assertThat(correctedStringField).isEqualTo("testing");
    assertThat(correctedSubStringField).isEqualTo("testing");
    assertThat(uncorrectedStringField).isEqualTo("no synonyms");
  }

  @Test
  @DisplayName("Validate handling of attempting to correct Entity with null dependency")
  void getCorrectedEntityWithNullDependency() throws ReflectiveOperationException {

    final TestCorrectionEntity testEntity = new TestCorrectionEntity("checking", null);
    Log.i(LOG_TAG, "Created test Entity: " + testEntity);

    correctionService.correctFields(testEntity);

    Log.i(LOG_TAG, "Entity after performing correction:  " + testEntity);
    assertThat(testEntity).isNotNull();
    assertThat(testEntity.getCorrectedStringField()).isEqualTo("testing");
    assertThat(testEntity.getSubEntity()).isNull();
  }

  @Test
  @DisplayName("Test correction with null String value marked as @Corrected")
  void nullStringValue() throws ReflectiveOperationException {

    final TestCorrectionEntity testEntity = new TestCorrectionEntity(null, null);
    Log.i(LOG_TAG, "Created Entity for testing:  " + testEntity);

    try {
      correctionService.correctFields(testEntity);
      throw new InternalError("If you can read this, something is wrong in the test logic!");

    } catch (IllegalArgumentException e) {
      Log.i(LOG_TAG, "Attempting to correct null marked as @Corrected threw: " + e);
      Log.i(LOG_TAG, "Entity after correction:  " + testEntity);
      assertThat(e.getClass()).isEqualTo(IllegalArgumentException.class);
    }
  }

  @Test
  @DisplayName("Validate that a word with no Synonyms is not modified")
  void testNoSynonymNoModify() throws ReflectiveOperationException {

    final String testString = "I have no equal.";
    final TestCorrectionEntity testEntity = new TestCorrectionEntity(testString);
    correctionService.createSynonymsFrom(getTestSynonymList());
    Log.i(LOG_TAG, "Created test resource:  " + testEntity);

    correctionService.correctFields(testEntity);
    Log.i(LOG_TAG, "Test resource after correction is:  " + testEntity);

    assertThat(testEntity).isNotNull();
    assertThat(testEntity.getSubEntity()).isNull();
    assertThat(testEntity.getCorrectedStringField()).isEqualTo(testString);
  }

  @Test
  @DisplayName("Validate correction of Event entity type")
  void validateEventCorrection() throws ReflectiveOperationException {

    // Create test data
    final Synonym barcaSynonyms =
        correctionService.createSynonymsFrom(List.of("FC Barcelona", "Barcelona", "Barca"));
    final Synonym romaSynonyms = correctionService.createSynonymsFrom(List.of("AS Roma", "Roma"));
    final Synonym laLigaSynonyms =
        correctionService.createSynonymsFrom(List.of("La Liga", "La Liga Santander", "LL"));
    final Event testMatch =
        Event.builder()
            .competition(new Competition("LL"))
            .homeTeam(new Team("Barca"))
            .awayTeam(new Team("Roma"))
            .season(new Season())
            .fixture(new Fixture())
            .date(LocalDateTime.now())
            .build();

    Log.i(
        LOG_TAG,
        String.format(
            "Created Synonyms: %n%s%n%s%n%s", barcaSynonyms, romaSynonyms, laLigaSynonyms));
    Log.i(LOG_TAG, "Testing Match:\n" + testMatch);

    correctionService.correctFields(testMatch);

    Log.i(LOG_TAG, "Correction performed; Match is now:\n" + testMatch);

    assertThat(testMatch).isNotNull();
    assertThat(testMatch.getCompetition().getName()).isEqualTo("La Liga");
    assertThat(testMatch.getHomeTeam().getName()).isEqualTo("FC Barcelona");
    assertThat(testMatch.getAwayTeam().getName()).isEqualTo("AS Roma");
    assertThat(testMatch.getSeason()).isNotNull();
    assertThat(testMatch.getFixture().getFixtureNumber()).isZero();
    assertThat(testMatch.getDate().toLocalDate()).isEqualTo(LocalDate.now());
  }

  @Test
  @DisplayName("Validate correction service does not corrupt unmatched entity")
  void testCorrectionOfUnmatchedEntity() throws ReflectiveOperationException {

    // Create test data
    final Synonym barcaSynonyms =
        correctionService.createSynonymsFrom(List.of("FC Barcelona", "Barcelona", "Barca"));
    final Synonym romaSynonyms = correctionService.createSynonymsFrom(List.of("AS Roma", "Roma"));
    final Synonym laLigaSynonyms =
        correctionService.createSynonymsFrom(List.of("La Liga", "La Liga Santander", "LL"));
    final String uncorrectedCompetitionName = "EPL";
    final String uncorrectedHomeTeamName = "Arsenal";
    final String uncorrectedAwayTeamName = "Chelsea";
    final Event testMatch =
        Event.builder()
            .competition(new Competition(uncorrectedCompetitionName))
            .homeTeam(new Team(uncorrectedHomeTeamName))
            .awayTeam(new Team(uncorrectedAwayTeamName))
            .season(new Season())
            .fixture(new Fixture())
            .date(LocalDateTime.now())
            .build();

    Log.i(
        LOG_TAG,
        String.format(
            "Created Synonyms: %n%s%n%s%n%s", barcaSynonyms, romaSynonyms, laLigaSynonyms));
    Log.i(LOG_TAG, "Testing Match:\n" + testMatch);

    correctionService.correctFields(testMatch);

    Log.i(LOG_TAG, "Correction performed; Match is now:\n" + testMatch);

    assertThat(testMatch).isNotNull();
    assertThat(testMatch.getCompetition().getName()).isEqualTo(uncorrectedCompetitionName);
    assertThat(testMatch.getHomeTeam().getName()).isEqualTo(uncorrectedHomeTeamName);
    assertThat(testMatch.getAwayTeam().getName()).isEqualTo(uncorrectedAwayTeamName);
    assertThat(testMatch.getSeason()).isNotNull();
    assertThat(testMatch.getFixture().getFixtureNumber()).isZero();
    assertThat(testMatch.getDate().toLocalDate()).isEqualTo(LocalDate.now());
  }

  @Test
  @DisplayName("Validate rejection of Event with null Competition")
  void rejectNullCompetition() {

    final Synonym synonym = new Synonym(List.of("Team", "Equipo", "Squad"));
    Log.i(LOG_TAG, "Adding Synonym for test:  " + synonym);
    correctionService.addSynonym(synonym);

    final Event testEvent =
        Event.builder()
            // no Competition...
            .homeTeam(new Team("Squad"))
            .awayTeam(new Team("Equipo"))
            .season(new Season())
            .fixture(new Fixture())
            .date(LocalDateTime.now())
            .build();
    try {
      Log.i(LOG_TAG, "Performing test on Event:\n" + testEvent);
      correctionService.correctFields(testEvent);

      Log.e(LOG_TAG, "We shouldn't even be here!");
      throw new RuntimeException("Test should have failed, but didn't");
    } catch (ReflectiveOperationException | IllegalArgumentException e) {
      Log.i(LOG_TAG, "Caught exception: " + e);
      assertThat(e.getClass()).isEqualTo(IllegalArgumentException.class);
    }
  }

  @Test
  @DisplayName("Validate creation of test resources")
  void validateTestResources() {

    final TestCorrectionEntity emptyTestEntity = new TestCorrectionEntity();
    final TestSubCorrectionEntity emptySubEntity = new TestSubCorrectionEntity();
    emptySubEntity.setCorrectedStringField("Correct me, please.");
    emptySubEntity.setUncorrectedString("No correcting me!");
    emptyTestEntity.setCorrectedStringField("testing");
    emptyTestEntity.setSubEntity(emptySubEntity);

    Log.i(LOG_TAG, "Created Entities with empty constructors:\n" + emptyTestEntity);
    assertThat(emptyTestEntity).isNotNull();
    assertThat(emptySubEntity).isNotNull();
    assertThat(emptyTestEntity.getCorrectedStringField()).isNotNull();
    assertThat(emptyTestEntity.getSubEntity()).isNotNull();
    assertThat(emptySubEntity.getCorrectedStringField()).isNotNull();
    assertThat(emptySubEntity.getUncorrectedString()).isNotNull();

    // exercise test classes
    emptyTestEntity.setId(Long.MAX_VALUE);
    Log.i(LOG_TAG, "Empty test entity ID set to: " + emptyTestEntity.getId());
    emptySubEntity.setId(Long.MIN_VALUE);
    Log.i(LOG_TAG, "Empty test sub entity ID set to: " + emptySubEntity.getId());
  }

  @Entity
  static class TestCorrectionEntity {

    @Id @GeneratedValue private Long id;

    @Corrected private String correctedStringField;

    @CorrectedOrNull
    @OneToOne(targetEntity = TestSubCorrectionEntity.class)
    private TestSubCorrectionEntity subEntity;

    TestCorrectionEntity(String correctedStringField, TestSubCorrectionEntity subEntity) {
      this.correctedStringField = correctedStringField;
      this.subEntity = subEntity;
    }

    TestCorrectionEntity(String correctedStringField) {
      this(correctedStringField, null);
    }

    TestCorrectionEntity() {
      this(null, null);
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getCorrectedStringField() {
      return this.correctedStringField;
    }

    public void setCorrectedStringField(String correctedStringField) {
      this.correctedStringField = correctedStringField;
    }

    public TestSubCorrectionEntity getSubEntity() {
      return this.subEntity;
    }

    public void setSubEntity(TestSubCorrectionEntity subEntity) {
      this.subEntity = subEntity;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
      TestCorrectionEntity that = (TestCorrectionEntity) o;
      return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
      return getClass().hashCode();
    }

    @Override
    public String toString() {
      return String.format(
          "TestCorrectionEntity(id=%s;  stringField=%s;  SubEntity=%s)",
          id, correctedStringField, subEntity);
    }
  }

  @Entity
  static class TestSubCorrectionEntity {

    @Id @GeneratedValue private Long id;

    @Corrected private String correctedStringField;

    private String uncorrectedString;

    TestSubCorrectionEntity(String correctedStringField, String uncorrectedString) {
      this.correctedStringField = correctedStringField;
      this.uncorrectedString = uncorrectedString;
    }

    TestSubCorrectionEntity() {
      this(null, null);
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getCorrectedStringField() {
      return this.correctedStringField;
    }

    public void setCorrectedStringField(String correctedStringField) {
      this.correctedStringField = correctedStringField;
    }

    public String getUncorrectedString() {
      return uncorrectedString;
    }

    public void setUncorrectedString(String uncorrectedString) {
      this.uncorrectedString = uncorrectedString;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
      TestSubCorrectionEntity that = (TestSubCorrectionEntity) o;
      return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
      return getClass().hashCode();
    }

    @Override
    public String toString() {
      return String.format(
          "SubCorrectionEntity{id=%s;  correctedStringField=%s;  uncorrectedString=%s)",
          id, correctedStringField, uncorrectedString);
    }
  }
}
