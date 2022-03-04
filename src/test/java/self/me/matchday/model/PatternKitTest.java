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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.util.Log;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validation for PatternKit<> class")
class PatternKitTest {

  private static final String LOG_TAG = "PatternKitTest";

  private static TestDataCreator testDataCreator;

  @BeforeAll
  static void setup(@Autowired @NotNull TestDataCreator testDataCreator) {
    PatternKitTest.testDataCreator = testDataCreator;
  }

  private static @NotNull Stream<Arguments> getPatternKitArgs() {

    final String regex =
        "([\\p{L}\\d\\s]+) (\\d{2,4}/\\d{2,4})[-Matchdy\\s]*(\\d+)[\\s-]*([\\p{L}\\s-]+) "
            + "vs.? ([\\p{L}\\s-]+) - (\\d{2}/\\d{2}/\\d{2,4})";

    return Stream.of(
        Arguments.of(
            testDataCreator.createFileSourcePatternKitManually(),
            testDataCreator.createFileSourcePatternFromFile()),
        Arguments.of(
            testDataCreator.createEventPatternKitManually(regex),
            testDataCreator.createEventPatternKitFromFile()));
  }

  @ParameterizedTest
  @MethodSource("getPatternKitArgs")
  @DisplayName("Test similarity of programmatically created PatternKits vs. those read from JSON")
  void testFileAndManualPatternEquals(PatternKit<?> manualKit, PatternKit<?> fromFileKit) {

    Log.i(LOG_TAG, "Manually created Kit:\n" + manualKit);
    Log.i(LOG_TAG, "Kit from file:\n" + fromFileKit);

    assertThat(manualKit).isNotNull();
    assertThat(fromFileKit).isNotNull();

    assertThat(manualKit.getPattern().flags()).isEqualTo(fromFileKit.getPattern().flags());
    assertThat(manualKit.getFields()).isEqualTo(fromFileKit.getFields());
    assertThat(manualKit.getClazz()).isEqualTo(fromFileKit.getClazz());
  }
}
