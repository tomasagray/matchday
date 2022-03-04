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

package self.me.matchday.model;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFilePack;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.util.JsonParser;
import self.me.matchday.util.Log;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validate PatternKit packing")
class PatternKitPackTest {

  private static final String LOG_TAG = "PatternKitPackTest";

  private static TestDataCreator testDataCreator;

  @BeforeAll
  static void setup(@Autowired TestDataCreator testDataCreator) {
    PatternKitPackTest.testDataCreator = testDataCreator;
  }

  @NotNull
  private PatternKitPack createPatternKitPack() {
    // create pattern kits
    final PatternKit<Event> eventPatternKit = testDataCreator.createEventPatternKit();
    final PatternKit<VideoFileSource> fileSourcePatternKit =
        testDataCreator.createFileSourcePatternKit();
    final PatternKit<VideoFile> videoFilePatternKit = testDataCreator.createVideoFilePatternKit();
    final PatternKit<VideoFilePack> videoFilePackPatternKit =
        testDataCreator.createVideoFilePackPatternKit();

    final PatternKitPack patternKitPack = new PatternKitPack();
    patternKitPack.addPatternKit(eventPatternKit);
    patternKitPack.addPatternKit(fileSourcePatternKit);
    patternKitPack.addPatternKit(videoFilePatternKit);
    patternKitPack.addPatternKit(videoFilePackPatternKit);
    return patternKitPack;
  }

  @Test
  @DisplayName(
      "Test PatternKitPack can be created and a PatternKit for a specific class retrieved from it")
  void testEventPatternKitPacking() {

    final PatternKitPack patternKitPack = createPatternKitPack();

    Log.i(LOG_TAG, "PatternKitPack is:\n" + patternKitPack);
    assertThat(patternKitPack.getPatternKits()).isNotNull();

    final List<PatternKit<? extends Event>> kitsForEvents =
        patternKitPack.getPatternKitsFor(Event.class);
    assertThat(kitsForEvents).isNotNull().isNotEmpty();
    assertThat(kitsForEvents.size()).isEqualTo(1);
    Log.i(LOG_TAG, "Kits for Events:\n" + kitsForEvents);
  }

  @Test
  @DisplayName("Test JSON encoding/decoding of PatternKitPacks")
  void jsonEncodePatternKitPack() {

    final PatternKitPack patternKitPack = createPatternKitPack();
    final Type type = new TypeReference<PatternKitPack>() {}.getType();

    final String json = JsonParser.toJson(patternKitPack, type);
    Log.i(LOG_TAG, "Got encoded JSON:\n" + json);

    assertThat(json).isNotNull().isNotEmpty();
    final PatternKitPack reconstitutedPack = JsonParser.fromJson(json, type);
    Log.i(LOG_TAG, "Reconstituted PatternKitPack as:\n" + reconstitutedPack);
    final Map<String, PatternKitEntry<?>> patternKits = reconstitutedPack.getPatternKits();
    assertThat(patternKits).isNotNull();
    assertThat(patternKits.get(Event.class.getName())).isNotNull();
  }

  @Test
  @DisplayName("Overstuff a PatternKitPack")
  void overstuffed() {

    final int expectedEventPatternKitCount = 2;

    final PatternKitPack patternKitPack = createPatternKitPack();
    final PatternKit<Event> eventPatternKit = testDataCreator.createEventPatternKit();
    Log.i(LOG_TAG, "Adding PatternKit<Event> to PatternKitPack...");

    patternKitPack.addPatternKit(eventPatternKit);
    Log.i(LOG_TAG, "PatternKitPack is now:\n" + patternKitPack);
    final List<PatternKit<? extends Event>> eventPatternKits =
        patternKitPack.getPatternKitsFor(Event.class);
    Log.i(LOG_TAG, "Event PatternKits:\n" + eventPatternKits);

    assertThat(eventPatternKits).isNotNull();
    assertThat(eventPatternKits.size()).isEqualTo(expectedEventPatternKitCount);
  }

  @Test
  @DisplayName("Test attempting to get non-existent class from Pack")
  void getNonExistent() {

    final PatternKitPack patternKitPack = createPatternKitPack();
    Class<?> missingClass = IllegalArgumentException.class;
    Log.i(
        LOG_TAG,
        String.format(
            "Attempting to get class: %s from PatternKitPack:%n%s", missingClass, patternKitPack));

    final List<? extends PatternKit<?>> ghostPatternKits =
        patternKitPack.getPatternKitsFor(missingClass);
    Log.i(LOG_TAG, "These PatternKits should be null:\n" + ghostPatternKits);
    assertThat(ghostPatternKits).isNull();
  }

  @Test
  @DisplayName("Validate bulk adding of Collection of PatternKits")
  void testAddAllPatternKits() {

    final int expectedPatternKitCount = 4;

    final PatternKitPack patternKitPack = new PatternKitPack();
    final List<PatternKit<?>> patternKits =
        List.of(
            testDataCreator.createEventPatternKit(),
            testDataCreator.createFileSourcePatternKit(),
            testDataCreator.createVideoFilePatternKit(),
            testDataCreator.createVideoFilePackPatternKit());
    patternKitPack.addAllPatternKits(patternKits);

    // collate
    final List<? extends PatternKit<?>> patternKitList =
        patternKitPack.getPatternKits().values().stream()
            .map(PatternKitEntry::getPatternKits)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    assertThat(patternKitList).isNotNull();
    final int actualPatternKitCount = patternKitList.size();
    Log.i(LOG_TAG, "Total PatternKit count after bulk adding: " + actualPatternKitCount);
    assertThat(actualPatternKitCount).isNotZero().isEqualTo(expectedPatternKitCount);
  }
}
