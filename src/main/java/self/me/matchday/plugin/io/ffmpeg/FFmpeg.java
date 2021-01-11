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

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FFmpeg {

  private static final String SEGMENT_PATTERN = "segment_%05d.ts";

  private final List<String> baseArgs;
  @Getter @Setter boolean loggingEnabled = true;

  FFmpeg(@NotNull final String execPath) {
    this.baseArgs =
        List.of(
            String.format("\"%s\"", execPath),
            "-v info",
            "-y",
            "-protocol_whitelist concat,file,http,https,tcp,tls,crypto");
  }

  /**
   * Create a concatenated FFMPEG stream of the given URIs at the given storage location
   *
   * @param uris A List of file resource pointers
   * @param location The location on disk to store stream data
   * @return A thread task
   */
  public FFmpegStreamTask getHlsStreamTask(@NotNull final Path location, @NotNull final URI... uris) {

    // Assemble arguments
    final String command = Strings.join(baseArgs, ' ');
    final Path outputPath = location.toAbsolutePath();
    final Path dataDir = outputPath.getParent();
    final List<String> transcodeArgs = getDefaultTranscodeArgs(location);

    if (uris.length > 1) {
      // Create FFMPEG CLI command & return
      return FFmpegConcatStreamTask.builder()
              .command(command)
              .uris(List.of(uris))
              .transcodeArgs(transcodeArgs)
              .outputPath(outputPath)
              .dataDir(dataDir)
              .loggingEnabled(loggingEnabled)
              .build();
    } else {
      URI uri = uris[0];
      // Create streaming task & return
      return FFmpegSingleStreamTask.builder()
              .command(command)
              .uri(uri)
              .transcodeArgs(transcodeArgs)
              .outputFile(outputPath)
              .dataDir(dataDir)
              .loggingEnabled(loggingEnabled)
              .build();
    }
  }

  private List<String> getDefaultTranscodeArgs(@NotNull final Path playlistPath) {

    final List<String> transcodeArgs = new ArrayList<>();
    final Path storageLocation = playlistPath.getParent().toAbsolutePath();
    final Path segmentPattern = storageLocation.resolve(SEGMENT_PATTERN);
    // Add arguments
    transcodeArgs.add("-vcodec copy");
    transcodeArgs.add("-acodec copy");
    transcodeArgs.add("-muxdelay 0");
    transcodeArgs.add("-f hls");
    transcodeArgs.add("-hls_playlist_type event");
    transcodeArgs.add("-hls_segment_filename");
    // Add segment output pattern
    final String segments = String.format("\"%s\"", segmentPattern);
    transcodeArgs.add(segments);

    return transcodeArgs;
  }
}
