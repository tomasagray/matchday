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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.util.ResourceFileReader;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("FFmpegTest - Test the creation of FFmpegSingleStreamTask HLS streaming task")
class FFmpegTest {

  private static final Logger logger = LogManager.getLogger(FFmpegTest.class);

  private static final String FFMPEG_EXE = "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe";
  private static final String DEFAULT_ARGS =
      " -v info -y -protocol_whitelist concat,file,http,https,tcp,tls,crypto";

  // Test resources
  private static FFmpegStreamTask hlsStreamTask;
  private static String storageLocation;

  @BeforeAll
  static void setUp(@Autowired @NotNull final TestDataCreator testDataCreator)
      throws URISyntaxException {

    logger.info("Instantiating FFmpeg Plugin with executable: " + FFMPEG_EXE);
    FFmpeg ffmpeg = new FFmpeg(FFMPEG_EXE);

    // Read configuration resources
    final Map<String, String> resources =
        ResourceFileReader.readPropertiesResource("video.properties");

    // Create URLs
    final URL firstHalfUrl = testDataCreator.getFirstHalfUrl();
    final URL secondHalfUrl = testDataCreator.getSecondHalfUrl();
    assertThat(firstHalfUrl).isNotNull();
    assertThat(secondHalfUrl).isNotNull();

    final List<URI> urls = List.of(firstHalfUrl.toURI(), secondHalfUrl.toURI());
    storageLocation = resources.get("video-resources.file-storage-location");
    hlsStreamTask = ffmpeg.getHlsStreamTask(Path.of(storageLocation), urls.toArray(new URI[0]));
  }

  @Test
  @DisplayName("Ensure task has correct # of arguments")
  void minArgLength() {

    // Test params
    final int minArgLength = 7;

    // Retrieve data from task
    final List<String> args = hlsStreamTask.getTranscodeArgs();

    // Test
    logger.info("Testing args: " + args);
    assertThat(args.size()).isGreaterThanOrEqualTo(minArgLength);
  }

  @Test
  @DisplayName("Check command default format")
  void testCommandFormat() {

    final String expectedCommand = String.format("\"%s\"%s", FFMPEG_EXE, DEFAULT_ARGS);
    final String actualCommand = hlsStreamTask.getCommand();

    logger.info("Testing command: " + actualCommand);
    assertThat(actualCommand).isEqualTo(expectedCommand);
  }

  @Test
  @DisplayName("Verify output path")
  void outputPath() {

    final Path expectedOutputPath = Path.of(FFmpegTest.storageLocation);
    final Path actualOutputPath = hlsStreamTask.getPlaylistPath();

    logger.info("Testing output path: " + actualOutputPath);
    assertThat(actualOutputPath).isEqualByComparingTo(expectedOutputPath);
  }
}
