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

import lombok.Data;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Data
public class FFmpegTask implements Runnable {

  private final String command;
  private final List<String> args;
  private final Path outputFile;
  private Process process;

  public FFmpegTask(
      @NotNull final String command,
      @NotNull final List<String> args,
      @NotNull final Path outputFile) {

    this.command = command;
    this.args = args;
    this.outputFile = outputFile;
  }

  @SneakyThrows
  @Override
  public void run() {

    try {
      // Create output directory
      Files.createDirectories(outputFile.getParent());
      // Collate program arguments
      final String arguments = Strings.join(args, ' ');
      final String execCommand = String.format("%s %s \"%s\"", command, arguments, outputFile);
      process = Runtime.getRuntime().exec(execCommand);
      // Allow the process to finish executing
      process.waitFor();
      process.destroy();
    } catch (InterruptedException e) {
      if (process != null) {
        Log.i("FFmpegTask", String.format("Streaming task with PID: %s interrupted", process.pid()));
      }
    }
  }

  public boolean kill() {

    // Ensure process exists
    if (process != null) {
      // Ensure process is dead
      return !(process.destroyForcibly().isAlive());
    }
    return true;
  }
}
