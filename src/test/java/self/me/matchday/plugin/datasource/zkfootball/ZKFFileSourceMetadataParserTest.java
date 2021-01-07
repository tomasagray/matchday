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

import org.assertj.core.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventFileSource.Resolution;
import self.me.matchday.model.FileSize;
import self.me.matchday.model.MD5String;
import self.me.matchday.util.Log;
import self.me.matchday.util.ResourceFileReader;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validate ZKF file source metadata parsing")
class ZKFFileSourceMetadataParserTest {

  private static final String LOG_TAG = "ZKFFileSourceMetadataParserTest";

  private static EventFileSource testFileSource;

  @BeforeAll
  static void setUp(@Autowired final ZKFPatterns zkfPatterns) throws IOException {

    final String metadataHtml =
        Strings.join(
                ResourceFileReader.readTextResource(
                    ZKFFileSourceMetadataParserTest.class, "zkf_source_metadata.html"))
            .with("\n");

    // Parse test data
    final Document document = Jsoup.parse(metadataHtml);
    final ZKFFileMetadata fileMetadata =
        new ZKFFileMetadata(document.getAllElements(), zkfPatterns);
    testFileSource = EventFileSource.createEventFileSource(fileMetadata, MD5String.generate());
  }

  @Test
  @DisplayName("Validate bitrate parsing")
  void testBitrate() {

    final Long actualBitrate = testFileSource.getBitrate();
    final Long expectedBitrate = 7_000_000L;

    Log.i(LOG_TAG, "Testing bitrate: " + actualBitrate);
    assertThat(actualBitrate).isEqualTo(expectedBitrate);
  }

  @Test
  @DisplayName("Validate channel parsing")
  void testChannel() {

    final String actualChannel = testFileSource.getChannel();
    final String expectedChannel = "Bein Sports HD";

    Log.i(LOG_TAG, "Testing channel: " + actualChannel);
    assertThat(actualChannel).isEqualTo(expectedChannel);
  }

  @Test
  @DisplayName("Validate approximate file size parsing")
  void testFileSize() {

    final Long actualFileSize = testFileSource.getFileSize();
    final Long expectedFileSize = FileSize.ofGigabytes(6);

    Log.i(LOG_TAG, "Testing approximate file size: " + actualFileSize);
    assertThat(actualFileSize).isEqualTo(expectedFileSize);
  }

  @Test
  @DisplayName("Validate framerate parsing")
  void testFramerate() {

    final int actualFramerate = testFileSource.getFrameRate();
    final int expectedFramerate = 60;

    Log.i(LOG_TAG, "Testing framerate: " + actualFramerate);
    assertThat(actualFramerate).isEqualTo(expectedFramerate);
  }

  @Test
  @DisplayName("Validate commentary language parsing")
  void testLanguages() {

    final String actualLanguages = testFileSource.getLanguages();
    final String expectedLanguages = "English";

    Log.i(LOG_TAG, "Testing commentary languages: " + actualLanguages);
    assertThat(actualLanguages).isEqualTo(expectedLanguages);
  }

  @Test
  @DisplayName("Validate media container parsing")
  void testMediaContainer() {

    final String actualMediaContainer = testFileSource.getMediaContainer();
    final String expectedMediaContainer = "MKV";

    Log.i(LOG_TAG, "Testing media container: " + actualMediaContainer);
    assertThat(actualMediaContainer).isEqualTo(expectedMediaContainer);
  }

  @Test
  @DisplayName("Validate video resolution parsing")
  void testVideoResolution() {

    final Resolution actualResolution = testFileSource.getResolution();
    final Resolution expectedResolution = Resolution.R_720p;

    Log.i(LOG_TAG, "Testing video resolution: " + actualResolution);
    assertThat(actualResolution).isEqualTo(expectedResolution);
  }
}
