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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class FFprobe {

  private final List<String> args;
  private final Gson gson;

  public FFprobe(@NotNull final String execPath) {

    // Create JSON parser
    this.gson = new Gson();
    // Setup global CLI arguments
    args = List.of(
        execPath,
        "-hide_banner",
        "-print_format json",
        "-show_streams",
        "-show_format",
        "-show_chapters"
    );
  }

  public FFmpegMetadata getFileMetadata(@NotNull final URI uri) throws IOException {

    // Result container
    String result;
    // Args for this job
    List<String> processArgs = new ArrayList<>(args);
    // Add remote URL to job args
    processArgs.add(uri.toString());

    // Create process for job
    final String cmd = String.join(" ", processArgs);
    Process p = Runtime.getRuntime().exec(cmd);
    int processResult;

    // Fetch remote data
    try (InputStream is = p.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      // Read data and collect as a String
      result =
          reader
              .lines()
              .collect(Collectors.joining(""));
    } finally {
      processResult = p.exitValue();
      // Ensure process closed
      p.destroy();
    }

    // Ensure JSON is valid
    try {
      return
          gson.fromJson(result, FFmpegMetadata.class);

    } catch (JsonSyntaxException e) {
      throw new IOException(
          String.format("Could not parse JSON from String, exit value: %s", processResult), e);
    }
  }
}
