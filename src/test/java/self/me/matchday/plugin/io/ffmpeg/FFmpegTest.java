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

package self.me.matchday.plugin.io.ffmpeg;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import self.me.matchday.CreateTestData;
import self.me.matchday.MatchdayApplication;
import self.me.matchday.util.ResourceFileReader;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FFmpegTest - Test the creation of FFmpegSingleStreamTask HLS streaming task")
class FFmpegTest {

  // Test constants
  private static final String LOG_TAG = "FFmpegTest";
  private static final String FFMPEG_EXE = "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe";
  private static final String DEFAULT_ARGS =
      " -v info -y -protocol_whitelist concat,file,http,https,tcp,tls,crypto";

  // Test resources
  private static FFmpegStreamTask hlsStreamTask;
  private static String storageLocation;

  @BeforeAll
  static void setUp() throws URISyntaxException, IOException {

    FFmpeg ffmpeg = new FFmpeg(FFMPEG_EXE);

    // Read configuration resources
    final Map<String, String> resources =
        ResourceFileReader.readPropertiesResource(
            MatchdayApplication.class, "video-resources.properties");

    // Create URLs
    final URL firstHalfUrl = CreateTestData.getFirstHalfUrl();
    final URL secondHalfUrl = CreateTestData.getSecondHalfUrl();
    assertThat(firstHalfUrl).isNotNull();
    assertThat(secondHalfUrl).isNotNull();

    final List<URI> urls =
        List.of(firstHalfUrl.toURI(), secondHalfUrl.toURI());
    storageLocation = resources.get("video-resources.file-storage-location") + "\\test_out";

    hlsStreamTask = ffmpeg.getHlsStreamTask(urls, Path.of(storageLocation));
  }

  @Test
  @DisplayName("Ensure task has correct # of arguments")
  void minArgLength() {

    // Test params
    final int minArgLength = 7;

    // Retrieve data from task
    final List<String> args = hlsStreamTask.getTranscodeArgs();

    // Test
    Log.i(LOG_TAG, "Testing args: " + args);
    assertThat(args.size()).isGreaterThanOrEqualTo(minArgLength);
  }

  @Test
  @DisplayName("Check command default format")
  void testCommandFormat() {

    final String expectedCommand = String.format("\"%s\"%s", FFMPEG_EXE, DEFAULT_ARGS);
    final String actualCommand = hlsStreamTask.getCommand();

    Log.i(LOG_TAG, "Testing command: " + actualCommand);
    assertThat(actualCommand).isEqualTo(expectedCommand);
  }

  @Test
  @DisplayName("Verify output path")
  void outputPath() {

    String playlistPath = FFmpegTest.storageLocation + "\\playlist.m3u8";
    final Path expectedOutputPath = Path.of(playlistPath);
    final Path actualOutputPath = hlsStreamTask.getOutputFile();

    Log.i(LOG_TAG, "Testing output path: " + actualOutputPath);
    assertThat(actualOutputPath).isEqualByComparingTo(expectedOutputPath);
  }
}
