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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FFmpeg {

  private static final String SEGMENT_PL_NAME = "playlist.m3u8";
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

  FFmpegTask getHlsStreamTask(@NotNull List<URI> uris, @NotNull final Path location) {

    // Assemble arguments
    final String storage = location.toString();
    final Path outputFile = Paths.get(storage, SEGMENT_PL_NAME);
    final List<String> transcodeArgs = new ArrayList<>();
    final String segments = String.format("\"%s\"", Paths.get(storage, SEGMENT_PATTERN));

    // Add arguments
    transcodeArgs.add("-vcodec copy");
    transcodeArgs.add("-acodec copy");
    transcodeArgs.add("-muxdelay 0");
    transcodeArgs.add("-f hls");
    transcodeArgs.add("-hls_playlist_type event");
    transcodeArgs.add("-hls_segment_filename");
    transcodeArgs.add(segments);

    // Create FFMPEG CLI command & return
    return FFmpegTask.builder()
        .command(Strings.join(baseArgs, ' '))
        .uris(uris)
        .transcodeArgs(transcodeArgs)
        .outputFile(outputFile)
        .loggingEnabled(loggingEnabled)
        .build();
  }
}
