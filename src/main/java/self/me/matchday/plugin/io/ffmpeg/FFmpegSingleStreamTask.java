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

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class FFmpegSingleStreamTask extends FFmpegStreamTask {

  private static final DateTimeFormatter LOGFILE_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_hh-mm-ss");

  private final URI uri;

  @Builder
  public FFmpegSingleStreamTask(
      String execPath,
      List<String> ffmpegArgs,
      Path playlistPath,
      Path dataDir,
      boolean loggingEnabled,
      List<String> transcodeArgs,
      URI uri) {

    this.execPath = execPath;
    this.ffmpegArgs = ffmpegArgs;
    this.playlistPath = playlistPath;
    this.dataDir = dataDir;
    this.loggingEnabled = loggingEnabled;
    this.transcodeArgs = transcodeArgs;
    this.uri = uri;
  }

  @Override
  protected @NotNull List<String> getArguments() {
    final List<String> args = new ArrayList<>(getFfmpegArgs());
    args.addAll(getInputArg());
    args.addAll(transcodeArgs);
    args.add(getPlaylistPath().toString());
    return args;
  }

  /**
   * Create directory to hold all streaming data
   *
   * @throws IOException If there are any problems with stream preparation
   */
  @Override
  protected void prepareStream() throws IOException {
    // Create output directory
    Files.createDirectories(this.getDataDir());
  }

  @Override
  protected @NotNull List<String> getInputArg() {
    return List.of("-i", uri.toString());
  }
}
