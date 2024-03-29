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

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

public class FFmpeg {

  private static final String SEGMENT_PATTERN = "segment_%05d.ts";

  private final String execPath;
  private final List<String> baseArgs;
  @Getter @Setter boolean loggingEnabled = true;

  public FFmpeg(@NotNull final String execPath) {
    this.execPath = execPath;
    this.baseArgs =
        List.of("-v", "info", "-y", "-protocol_whitelist", "concat,file,http,https,tcp,tls,crypto");
  }

  /**
   * Create a concatenated FFMPEG stream of the given URIs at the given storage location
   *
   * @param uris A List of file resource pointers
   * @param playlistPath The location on disk to store stream data
   * @return A thread task
   */
  public FFmpegStreamTask getHlsStreamTask(
      @NotNull final Path playlistPath, @NotNull final URI @NotNull ... uris) {

    // Assemble arguments
    final Path playlistAbsolutePath = playlistPath.toAbsolutePath();
    final Path dataDir = playlistAbsolutePath.getParent();
    final List<String> transcodeArgs = getDefaultTranscodeArgs(dataDir);

    if (uris.length > 1) {
      // Create FFMPEG CLI command & return
      return FFmpegConcatStreamTask.builder()
          .execPath(execPath)
          .ffmpegArgs(baseArgs)
          .uris(List.of(uris))
          .transcodeArgs(transcodeArgs)
          .playlistPath(playlistAbsolutePath)
          .dataDir(dataDir)
          .loggingEnabled(loggingEnabled)
          .build();
    } else {
      URI uri = uris[0];
      // Create streaming task & return
      return FFmpegSingleStreamTask.builder()
          .execPath(execPath)
          .ffmpegArgs(baseArgs)
          .uri(uri)
          .transcodeArgs(transcodeArgs)
          .playlistPath(playlistAbsolutePath)
          .dataDir(dataDir)
          .loggingEnabled(loggingEnabled)
          .build();
    }
  }

  private @NotNull List<String> getDefaultTranscodeArgs(@NotNull final Path storageLocation) {

    final List<String> transcodeArgs = new ArrayList<>();
    final Path absoluteStorageLocation = storageLocation.toAbsolutePath();
    final Path segmentPattern = absoluteStorageLocation.resolve(SEGMENT_PATTERN);
    // Add arguments
    transcodeArgs.addAll(List.of("-vcodec", "copy"));
    transcodeArgs.addAll(List.of("-acodec", "copy"));
    transcodeArgs.addAll(List.of("-muxdelay", "0"));
    transcodeArgs.addAll(List.of("-f", "hls"));
    transcodeArgs.addAll(List.of("-hls_playlist_type", "event"));
    transcodeArgs.add("-hls_segment_filename");
    transcodeArgs.add(segmentPattern.toString());
    return transcodeArgs;
  }
}
