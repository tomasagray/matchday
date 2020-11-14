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
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventFileSource.Resolution;
import self.me.matchday.util.Log;
import self.me.matchday.util.ResourceFileReader;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validate Galataman file source metadata parsing")
class GManFileSourceMetadataParserTest {

  private static final String LOG_TAG = "GManEventFileSourceParserTest";

  private static EventFileSource testFileSource;

  @BeforeAll
  static void setUp(@Autowired final GManFileSourceMetadataParser fileSourceMetadataParser)
      throws IOException {

    // Read test data file
    final String postHtml =
        Strings.join(
                ResourceFileReader.readTextResource(
                    GManEventFileSourceParserTest.class, "gman_source_metadata.html"))
            .with("\n");

    Log.i(LOG_TAG, "Read test data:\n" + postHtml);

    // Create test data
    testFileSource = fileSourceMetadataParser.createFileSource(postHtml);
  }

  @Test
  @DisplayName("Validate approximate video duration parsing")
  void testApproximateDurationParsing() {

    final String actualApproximateDuration = testFileSource.getApproximateDuration();
    final String expectedApproximateDuration = "90min + Extras";

    Log.i(LOG_TAG, "Validating approximate video duration: " + actualApproximateDuration);
    assertThat(actualApproximateDuration).isEqualTo(expectedApproximateDuration);
  }

  @Test
  @DisplayName("Validate audio channel parsing")
  void testAudioChannelParsing() {

    final int actualAudioChannels = testFileSource.getAudioChannels();
    final int expectedAudioChannels = 2;

    Log.i(LOG_TAG, "Testing audio channels: " + actualAudioChannels);
    assertThat(actualAudioChannels).isEqualTo(expectedAudioChannels);
  }

  @Test
  @DisplayName("Validate audio codec parsing")
  void testAudioCodecParsing() {

    final String actualCodec = testFileSource.getAudioCodec();
    final String expectedCodec = "MP3";

    Log.i(LOG_TAG, "Testing audio codec: " + actualCodec);
    assertThat(actualCodec).isEqualTo(expectedCodec);
  }

  @Test
  @DisplayName("Validate bitrate parsing")
  void testBitrate() {

    final Long actualBitrate = testFileSource.getBitrate();
    final Long expectedBitrate = 4_000_000L;

    Log.i(LOG_TAG, "Testing bitrate: " + actualBitrate);
    assertThat(actualBitrate).isEqualTo(expectedBitrate);
  }

  @Test
  @DisplayName("Validate channel parsing")
  void testChannel() {

    final String actualChannel = testFileSource.getChannel();
    final String expectedChannel = "Sky Sports HD";

    Log.i(LOG_TAG, "Testing channel: " + actualChannel);
    assertThat(actualChannel).isEqualTo(expectedChannel);
  }

  @Test
  @DisplayName("Validate approximate file size parsing")
  void testFileSize() {

    final Long actualFileSize = testFileSource.getFileSize();
    final Long expectedFileSize = 3_221_225_472L;

    Log.i(LOG_TAG, "Testing approximate file size: " + actualFileSize);
    assertThat(actualFileSize).isEqualTo(expectedFileSize);
  }

  @Test
  @DisplayName("Validate framerate parsing")
  void testFramerate() {

    final int actualFramerate = testFileSource.getFrameRate();
    final int expectedFramerate = 25;

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

  @Test
  @DisplayName("Validate source parsing")
  void testSource() {

    final String actualSource = testFileSource.getSource();
    final String expectedSource = "DVB-S2";

    Log.i(LOG_TAG, "Testing source: " + actualSource);
    assertThat(actualSource).isEqualTo(expectedSource);
  }

  @Test
  @DisplayName("Validate video codec parsing")
  void testVideoCodec() {

    final String actualVideoCodec = testFileSource.getVideoCodec();
    final String expectedVideoCodec = "H.264";

    Log.i(LOG_TAG, "Testing video codec: " + actualVideoCodec);
    assertThat(actualVideoCodec).isEqualTo(expectedVideoCodec);
  }
}
