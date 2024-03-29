/*
 * Copyright (c) 2021.
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

/** Class which creates an FFMPEG concatenation task; concatenates multiple video files into one */
@EqualsAndHashCode(callSuper = true)
@Data
public final class FFmpegConcatStreamTask extends FFmpegStreamTask {

  private static final String CONCAT_FILENAME = "concat.txt";
  private final List<URI> uris;
  private Path concatFile;

  @Builder
  public FFmpegConcatStreamTask(
      String execPath,
      List<String> ffmpegArgs,
      Path playlistPath,
      Path dataDir,
      boolean loggingEnabled,
      List<String> transcodeArgs,
      List<URI> uris) {

    this.execPath = execPath;
    this.ffmpegArgs = ffmpegArgs;
    this.playlistPath = playlistPath;
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
    // Create URI list text file
    this.concatFile = createConcatFile();
  }

  @Override
  public @NotNull @Unmodifiable List<String> getInputArg() {
    return List.of("-f", "concat", "-safe", "0", "-i", getConcatFile().toString());
  }

  /**
   * Create the text file (concat.txt) used by FFMPEG for concatenation
   *
   * @return The path of the concat.txt file
   * @throws IOException If there is an error creating or writing the file
   */
  private Path createConcatFile() throws IOException {

    // Map each URI to en entry in the concat file
    final String concatFileText =
        uris.stream().map(url -> String.format("file '%s'\n", url)).collect(Collectors.joining());
    // Write data to file
    final Path concatFilePath = Path.of(getDataDir().toAbsolutePath().toString(), CONCAT_FILENAME);
    return Files.writeString(concatFilePath, concatFileText);
  }
}
