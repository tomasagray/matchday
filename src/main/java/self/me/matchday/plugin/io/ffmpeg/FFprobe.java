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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FFprobe {

  private final List<String> baseArgs;
  private final Gson gson;
  private String result;
  private int processResult;

  public FFprobe(@NotNull final String execPath) {

    // Create JSON parser
    this.gson = new Gson();
    // Setup global CLI arguments
    baseArgs =
        List.of(
            execPath,
            "-hide_banner",
            "-print_format json",
            "-show_streams",
            "-show_format",
            "-show_chapters");
  }

  /**
   * Retrieve metadata from an audio/video file
   *
   * @param uri The file resource pointer
   * @return The file metadata
   * @throws IOException If the metadata could not be read or parsed
   */
  public FFmpegMetadata getFileMetadata(@NotNull final URI uri) throws IOException {

    try {
      // Read remote file metadata
      readFileMetadata(uri);
      // Parse data from JSON to metadata object
      return gson.fromJson(result, FFmpegMetadata.class);

    } catch (JsonSyntaxException e) {
      throw new IOException(
          String.format("Could not parse JSON from String, exit value: %s", processResult), e);
    }
  }

  /**
   * Read audio/video file metadata from a URI
   *
   * @param uri The URI of the video file
   * @throws IOException If there is an error reading data
   */
  private void readFileMetadata(@NotNull final URI uri) throws IOException {

    // Assemble args for this job
    List<String> processArgs = new ArrayList<>(baseArgs);
    // Add remote URL to job args
    processArgs.add(uri.toString());
    // Create process for job
    final String cmd = String.join(" ", processArgs);
    Process process = Runtime.getRuntime().exec(cmd);

    // Fetch remote data
    try (InputStream is = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

      // Read data and collect as a String
      result = reader.lines().collect(Collectors.joining(""));

    } finally {
      // Ensure process closed
      processResult = process.exitValue();
      process.destroy();
    }
  }
}
