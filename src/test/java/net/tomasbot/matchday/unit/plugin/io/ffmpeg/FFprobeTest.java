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

package net.tomasbot.matchday.unit.plugin.io.ffmpeg;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import net.tomasbot.matchday.TestDataCreator;
import net.tomasbot.matchday.plugin.io.ffmpeg.FFmpegMetadata;
import net.tomasbot.matchday.plugin.io.ffmpeg.FFprobe;
import net.tomasbot.matchday.util.JsonParser;
import net.tomasbot.matchday.util.ResourceFileReader;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("FFprobeTest - Test the functionality of the FFprobe plugin")
@TestMethodOrder(OrderAnnotation.class)
class FFprobeTest {

  private static final Logger logger = LogManager.getLogger(FFprobeTest.class);

  private static final String SAMPLE_METADATA_JSON = "data/ffprobe_sample_metadata.json";

  private static FFprobe ffProbe;
  private static URL testUrl;
  private static FFmpegMetadata expectedMetadata;

  private final TestDataCreator testDataCreator;

  @Value("${plugin.ffmpeg.ffprobe-location}")
  private String ffprobePath;

  @Autowired
  FFprobeTest(TestDataCreator testDataCreator) throws IOException {
    this.testDataCreator = testDataCreator;
    // Read test metadata & deserialize
    String sampleMetadata = ResourceFileReader.readTextResource(SAMPLE_METADATA_JSON);
    // Parse JSON to object
    expectedMetadata = JsonParser.fromJson(sampleMetadata, FFmpegMetadata.class);
  }

  @Test
  @Order(1)
  @DisplayName("Check creation of FFprobe instance")
  void checkFFprobeCreation() {
    // Create FFprobe instance
    logger.info("Instantiating FFprobe with executable: {}", ffprobePath);
    ffProbe = new FFprobe(ffprobePath);
    FFprobeTest.testUrl = testDataCreator.getFirstHalfUrl();
    logger.info("Verifying FFprobe instance is NOT NULL...");
    assertThat(ffProbe).isNotNull();
  }

  @Test
  @Order(2)
  @DisplayName("Verify FFprobe can read remote file metadata")
  void testGetFileMetadata() throws URISyntaxException, IOException {
    final String baseUrl = testUrl.toString().replaceAll("\\?\\w*=[\\w-]*", "");
    logger.info("Reading file data from: {}", baseUrl);
    FFmpegMetadata actualMetadata = ffProbe.getFileMetadata(new URI(baseUrl));

    logger.info("Testing metadata for correctness...");
    assertThat(actualMetadata).isNotNull();
    assertThat(JsonParser.toJson(actualMetadata)).isEqualTo(JsonParser.toJson(expectedMetadata));
  }
}
