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

package self.me.matchday.plugin.io.ffmpeg;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.util.JsonParser;
import self.me.matchday.util.Log;
import self.me.matchday.util.ResourceFileReader;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("FFprobeTest - Test the functionality of the FFprobe plugin")
@TestMethodOrder(OrderAnnotation.class)
class FFprobeTest {

  private static final String LOG_TAG = "FFprobeTest";

  private static final String FFPROBE_PATH = "plugin.ffmpeg.ffprobe-location";
  private static final String FFMPEG_PROPERTIES = "plugins\\ffmpeg\\ffmpeg.properties";
  private static final String SAMPLE_METADATA_JSON = "data/ffprobe_sample_metadata.json";

  private static FFprobe ffProbe;
  private static URL testUrl;
  private static FFmpegMetadata expectedMetadata;

  @BeforeAll
  static void setUp(@Autowired @NotNull final TestDataCreator testDataCreator) {

    // Read plugin resources file
    Map<String, String> resources =
        ResourceFileReader.readPropertiesResource(FFprobe.class, FFMPEG_PROPERTIES);
    // Read test metadata & deserialize
    String sampleMetadata =
        ResourceFileReader.readTextResource(FFprobeTest.class, SAMPLE_METADATA_JSON);
    // Parse JSON to object
    expectedMetadata = JsonParser.fromJson(sampleMetadata, FFmpegMetadata.class);

    // Create FFprobe instance
    Log.i(LOG_TAG, "Instantiating FFprobe with executable: " + FFPROBE_PATH);
    ffProbe = new FFprobe(resources.get(FFPROBE_PATH));
    FFprobeTest.testUrl = testDataCreator.getFirstHalfUrl();
  }

  @Test
  @Order(1)
  @DisplayName("Check creation of FFprobe instance")
  void checkFFprobeCreation() {

    Log.i(LOG_TAG, "Verifying FFprobe instance is NOT NULL...");
    assertThat(ffProbe).isNotNull();
  }

  @Test
  @Order(2)
  @DisplayName("Verify FFprobe can read remote file metadata")
  void testGetFileMetadata() throws URISyntaxException, IOException {

    final String baseUrl = testUrl.toString().replaceAll("\\?\\w*=[\\w-]*", "");
    Log.i(LOG_TAG, "Reading file data from: " + baseUrl);
    FFmpegMetadata actualMetadata = ffProbe.getFileMetadata(new URI(baseUrl));

    Log.i(LOG_TAG, "Testing metadata for correctness...");
    assertThat(actualMetadata).isNotNull();
    assertThat(JsonParser.toJson(actualMetadata)).isEqualTo(JsonParser.toJson(expectedMetadata));
  }
}
