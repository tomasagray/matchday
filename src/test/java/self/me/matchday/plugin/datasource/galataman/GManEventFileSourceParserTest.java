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

package self.me.matchday.plugin.datasource.galataman;

import org.assertj.core.util.Strings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.CreateTestData;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventFileSource.Resolution;
import self.me.matchday.model.Match;
import self.me.matchday.util.Log;
import self.me.matchday.util.ResourceFileReader;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validate Galataman Event file source parsing")
class GManEventFileSourceParserTest {

  private static final String LOG_TAG = "GManEventFileSourceParserTest";
  private static List<EventFileSource> testFileSources;
  private static EventFileSource secondSource;
  private static EventFileSource firstSource;

  @BeforeAll
  static void setUp(@Autowired final GManEventFileSourceParser fileSourceParser)
      throws IOException {

    // Read test data
    final String postHtml =
        Strings.join(
                ResourceFileReader.readTextResource(
                    GManEventFileSourceParserTest.class, "gman_post.html"))
            .with("\n");
    Log.i(LOG_TAG, "Read file source test data:\n" + postHtml);

    // Create test event
    final Match testMatch = CreateTestData.createTestMatch();

    // Parse test data
    testFileSources = fileSourceParser.getEventFileSources(testMatch, postHtml);
    Log.i(LOG_TAG, "Parsed file source data: " + testFileSources);
    firstSource = testFileSources.get(0);
    secondSource = testFileSources.get(1);
  }

  @Test
  @DisplayName("Validate file source count")
  void testFileSourceCount() {

    // pre-test
    assertThat(testFileSources).isNotNull();

    final int actualFileSourceCount = testFileSources.size();
    final int expectedFileSourceCount = 3;

    Log.i(LOG_TAG, "Testing file source count: " + actualFileSourceCount);
    assertThat(actualFileSourceCount).isEqualTo(expectedFileSourceCount);
  }

  @Test
  @DisplayName("Validate parses correct # of file sources from sample data")
  void testEventFileSourceCount() {

    final int actualFileSourceCount = testFileSources.size();
    final int expectedFileSourceCount = 3;

    Log.i(LOG_TAG, "Testing file source count: " + actualFileSourceCount);
    assertThat(actualFileSourceCount).isEqualTo(expectedFileSourceCount);
  }

  @Test
  @DisplayName("Validate source channel parsing")
  void testSourceChannel() {

    final String actualChannel1 = firstSource.getChannel();
    final String actualChannel2 = secondSource.getChannel();
    final String expectedChannel1 = "Sky Sports HD";
    final String expectedChannel2 = "TVE La 1 HD";

    Log.i(
        LOG_TAG, String.format("Testing source channels: %s, %s", actualChannel1, actualChannel2));
    assertThat(actualChannel1).isEqualTo(expectedChannel1);
    assertThat(actualChannel2).isEqualTo(expectedChannel2);
  }

  @Test
  @DisplayName("Validate resolution parsing")
  void testSourceResolution() {

    final Resolution actualResolution1 = firstSource.getResolution();
    final Resolution actualResolution2 = secondSource.getResolution();
    final Resolution expectedResolution1 = Resolution.R_1080p;
    final Resolution expectedResolution2 = Resolution.R_1080i;

    Log.i(
        LOG_TAG,
        String.format("Testing source resolutions: %s, %s", actualResolution1, actualResolution2));
    assertThat(actualResolution1).isEqualTo(expectedResolution1);
    assertThat(actualResolution2).isEqualTo(expectedResolution2);
  }

  @Test
  @DisplayName("Validate language parsing")
  void testSourceLanguages() {

    final String actualLanguages1 = firstSource.getLanguages();
    final String actualLanguages2 = secondSource.getLanguages();
    final String expectedLanguages1 = "English";
    final String expectedLanguages2 = "Spanish";

    Log.i(
        LOG_TAG,
        String.format("Testing source languages: %s, %s", actualLanguages1, actualLanguages2));
    assertThat(actualLanguages1).isEqualTo(expectedLanguages1);
    assertThat(actualLanguages2).isEqualTo(expectedLanguages2);
  }

  @Test
  @DisplayName("Validate event file parsing")
  void testSourceFiles() {

    final int expectedFileCount = 4;
    final List<EventFile> actualEventFiles1 = firstSource.getEventFiles();
    final List<EventFile> actualEventFiles2 = secondSource.getEventFiles();

    assertThat(actualEventFiles1.size()).isEqualTo(expectedFileCount);
    assertThat(actualEventFiles2.size()).isEqualTo(expectedFileCount);
  }
}
