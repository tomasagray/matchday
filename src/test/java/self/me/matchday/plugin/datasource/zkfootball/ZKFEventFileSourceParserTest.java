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

package self.me.matchday.plugin.datasource.zkfootball;

import com.google.gson.Gson;
import lombok.Data;
import org.assertj.core.util.Strings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventFileSource.Resolution;
import self.me.matchday.util.Log;
import self.me.matchday.util.ResourceFileReader;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validate ZKF Event file source parsing")
class ZKFEventFileSourceParserTest {

  private static final String LOG_TAG = "ZKFEventFileSourceParserTest";

  // Test resources
  private static List<EventFileSource> testFileSources;
  private static EventFileSource firstSource;
  private static EventFileSource secondSource;

  // Mock class for testing purposes only!
  @Data
  private static class TestBloggerPost {

    private String version;
    private String encoding;
    private TestBloggerEntry entry;
    TestBloggerEntry getEntry() {
      return entry;
    }
    @Data
    static class TestBloggerEntryId {
      private String t;

      public void setT(String t) {
        this.t = t;
      }
    }
    @Data
    static class TestBloggerEntryType {
      private String type;
      private String text;

      String getText() {
        return text;
      }
    }
    @Data
    static class TestBloggerEntry {
      private TestBloggerEntryId id;
      private LocalDateTime published;
      private LocalDateTime updated;
      private TestBloggerEntryType title;
      private TestBloggerEntryType content;

      TestBloggerEntryType getContent() {
        return content;
      }
    }
  }

  @BeforeAll
  static void setUp(@Autowired final ZKFEventFileSourceParser fileSourceParser) throws IOException {

    final String zkfPostData =
        Strings.join(
                ResourceFileReader.readTextResource(
                    ZKFEventFileSourceParserTest.class, "zkf_post_modified.json"))
            .with("\n");

    // Parse test data
    final Gson gson = new Gson();
    final TestBloggerPost zkfPost = gson.fromJson(zkfPostData, TestBloggerPost.class);
    final String text = zkfPost.getEntry().getContent().getText();
    testFileSources = fileSourceParser.getEventFileSources(text);

    assertThat(testFileSources).isNotNull();
    Log.i(LOG_TAG, "Parsed file source data: " + testFileSources);
    firstSource = testFileSources.get(0);
    secondSource = testFileSources.get(1);
  }

  @Test
  @DisplayName("Validate parses correct # of file sources from sample data")
  void testEventFileSourceCount() {

    final int actualFileSourceCount = testFileSources.size();
    final int expectedFileSourceCount = 2;

    Log.i(LOG_TAG, "Testing file source count: " + actualFileSourceCount);
    assertThat(actualFileSourceCount).isEqualTo(expectedFileSourceCount);
  }

  @Test
  @DisplayName("Validate source channel parsing")
  void testSourceChannel() {

    final String actualChannel1 = firstSource.getChannel();
    final String actualChannel2 = secondSource.getChannel();
    final String expectedChannel1 = "ORF Eins HD";
    final String expectedChannel2 = "ESPN";

    Log.i(LOG_TAG, String.format("Testing source channels: %s, %s", actualChannel1, actualChannel2));
    assertThat(actualChannel1).isEqualTo(expectedChannel1);
    assertThat(actualChannel2).isEqualTo(expectedChannel2);
  }

  @Test
  @DisplayName("Validate resolution parsing")
  void testSourceResolution() {

    final Resolution actualResolution1 = firstSource.getResolution();
    final Resolution actualResolution2 = secondSource.getResolution();
    final Resolution expectedResolution = Resolution.R_720p;

    Log.i(LOG_TAG, String.format("Testing source resolutions: %s, %s", actualResolution1, actualResolution2));
    assertThat(actualResolution1).isEqualTo(expectedResolution);
    assertThat(actualResolution2).isEqualTo(expectedResolution);
  }

  @Test
  @DisplayName("Validate language parsing")
  void testSourceLanguages() {

    final List<String> actualLanguages1 = firstSource.getLanguages();
    final List<String> actualLanguages2 = secondSource.getLanguages();
    final List<String> expectedLanguages1 = List.of("German");
    final List<String> expectedLanguages2 = List.of("English");

    Log.i(LOG_TAG, String.format("Testing source languages: %s, %s", actualLanguages1, actualLanguages2));
    assertThat(actualLanguages1).isEqualTo(expectedLanguages1);
    assertThat(actualLanguages2).isEqualTo(expectedLanguages2);
  }

  @Test
  @DisplayName("Validate event file parsing")
  void testSourceFiles() {

    final int expectedFileCount = 1;
    final List<EventFile> actualEventFiles1 = firstSource.getEventFiles();
    final List<EventFile> actualEventFiles2 = secondSource.getEventFiles();

    assertThat(actualEventFiles1.size()).isEqualTo(expectedFileCount);
    assertThat(actualEventFiles2.size()).isEqualTo(expectedFileCount);
  }
}
