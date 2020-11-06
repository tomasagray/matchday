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

package self.me.matchday.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Abbreviation strategy validation")
class AbbreviatorTest {

    private static final String LOG_TAG = "AbbreviatorTest";

    private static final String TEST_STRING = "The quick brown fox jumped over the lazy dog.";

    @Test
    @DisplayName("Test default abbreviation strategy")
    void abbreviate() {

        final String actualAbbreviation = Abbreviator.abbreviate(TEST_STRING);
        final String expectedAbbreviation = "THE";

        Log.i(LOG_TAG, "Testing default abbreviation strategy for test String: " + TEST_STRING);
        assertThat(actualAbbreviation).isEqualTo(expectedAbbreviation);
    }

    @Test
    @DisplayName("Test custom abbreviation length")
    void testAbbreviate() {

        final String actualAbbreviation = Abbreviator.abbreviate(TEST_STRING, 10);
        final String expectedAbbreviation = "THEQUICKBR";

        Log.i(LOG_TAG, "Testing custom abbreviation of test String: " + TEST_STRING);
        assertThat(actualAbbreviation).isEqualTo(expectedAbbreviation);
    }
}