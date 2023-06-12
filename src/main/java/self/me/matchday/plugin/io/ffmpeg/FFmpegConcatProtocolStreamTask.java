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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

@EqualsAndHashCode(callSuper = true)
@Data
public final class FFmpegConcatProtocolStreamTask extends FFmpegStreamTask {

  private final List<URI> uris;

  @Builder
  public FFmpegConcatProtocolStreamTask(
      String execPath,
      List<String> ffmpegArgs,
      Path outputPath,
      Path dataDir,
      boolean loggingEnabled,
      List<String> transcodeArgs,
      List<URI> uris) {

    this.execPath = execPath;
    this.ffmpegArgs = ffmpegArgs;
    this.playlistPath = outputPath;
    this.dataDir = dataDir;
    this.loggingEnabled = loggingEnabled;
    this.transcodeArgs = transcodeArgs;
    this.uris = uris;
  }

  @Override
  public @NotNull List<String> getArguments() {
    final List<String> args = new ArrayList<>(getFfmpegArgs());
    args.addAll(getInputArg());
    args.addAll(transcodeArgs);
    args.add(getPlaylistPath().toString());
    return args;
  }

  @Override
  public void prepareStream() throws IOException {
    // Create output directory
    Files.createDirectories(this.getDataDir());
  }

  @Override
  public @NotNull @Unmodifiable List<String> getInputArg() {

    final List<String> uriStrings = uris.stream().map(URI::toString).collect(Collectors.toList());
    final String concatText = String.join("|", uriStrings);
    // Create input String & return
    final String concat = String.format("\"concat:%s\"", concatText);
    return List.of("-i", concat);
  }
}
