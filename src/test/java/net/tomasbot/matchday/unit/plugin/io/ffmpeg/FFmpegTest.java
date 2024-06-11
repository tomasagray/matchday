/*
 * Copyright (c) 2023.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import net.tomasbot.matchday.TestDataCreator;
import net.tomasbot.matchday.plugin.io.ffmpeg.FFmpeg;
import net.tomasbot.matchday.plugin.io.ffmpeg.FFmpegStreamTask;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("FFmpegTest - Test the creation of FFmpegSingleStreamTask HLS streaming task")
class FFmpegTest {

  private static final Logger logger = LogManager.getLogger(FFmpegTest.class);

  //  private static final String FFMPEG_PROPERTIES = "plugins/ffmpeg/ffmpeg.properties";
  private static final String DEFAULT_ARGS =
      "-v info -y -protocol_whitelist concat,file,http,https,tcp,tls,crypto";
  // Test resources
  private static FFmpegStreamTask hlsStreamTask;
  private final TestDataCreator testDataCreator;

  @Value("${DATA_ROOT}/videos")
  private String storageLocation;

  @Value("${plugin.ffmpeg.ffmpeg-location}")
  private String ffmpegLocation;

  @Autowired
  public FFmpegTest(TestDataCreator testDataCreator) {
    this.testDataCreator = testDataCreator;
  }

  @BeforeEach
  public void setup() throws URISyntaxException {
    if (hlsStreamTask == null) {
      FFmpeg ffmpeg = new FFmpeg(ffmpegLocation);
      // Create URLs
      final URL firstHalfUrl = testDataCreator.getFirstHalfUrl();
      final URL secondHalfUrl = testDataCreator.getSecondHalfUrl();
      assertThat(firstHalfUrl).isNotNull();
      assertThat(secondHalfUrl).isNotNull();
      final List<URI> urls = List.of(firstHalfUrl.toURI(), secondHalfUrl.toURI());
      hlsStreamTask = ffmpeg.getHlsStreamTask(Path.of(storageLocation), urls.toArray(new URI[0]));
    }
  }

  @Test
  @DisplayName("Ensure task has correct # of arguments")
  void minArgLength() throws URISyntaxException {
    setup();

    // Test params
    final int minArgLength = 7;

    // Retrieve data from task
    final List<String> args = hlsStreamTask.getTranscodeArgs();

    // Test
    logger.info("Testing args: {}", args);
    assertThat(args.size()).isGreaterThanOrEqualTo(minArgLength);
  }

  @Test
  @DisplayName("Check command default format")
  void testCommandFormat() {
    final String actualCommand = String.join(" ", hlsStreamTask.getFfmpegArgs());

    logger.info("Testing command: {}", actualCommand);
    assertThat(actualCommand).isEqualTo(DEFAULT_ARGS);
  }

  @Test
  @DisplayName("Verify output path")
  void outputPath() {
    final Path expectedOutputPath = Path.of(storageLocation);
    final Path actualOutputPath = hlsStreamTask.getPlaylistPath();

    logger.info("Testing output path: {}", actualOutputPath);
    assertThat(actualOutputPath).isEqualByComparingTo(expectedOutputPath);
  }
}
