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
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Log;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@Builder
public class FFmpegTask implements Runnable {

  public static final String LOG_TAG = "FFmpegTask";
  private static final DateTimeFormatter LOGFILE_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_hh-mm-ss");

  private final String command;
  private final List<String> args;
  private final Path outputFile;
  private final boolean loggingEnabled;
  private Process process;
  private FFmpegLogAdapter logAdapter;

  /*public FFmpegTask(
      @NotNull final String command,
      @NotNull final List<String> args,
      @NotNull final Path outputFile,
      final boolean loggingEnabled) {

    this.command = command;
    this.args = args;
    this.outputFile = outputFile;
    this.loggingEnabled = loggingEnabled;
  }*/

  @SneakyThrows
  @Override
  public void run() {

    try {
      // Create output directory
      final Path dataDir = outputFile.getParent();
      Files.createDirectories(dataDir);

      // Collate program arguments
      final String arguments = Strings.join(args, ' ');
      String execCommand = String.format("%s %s \"%s\"", command, arguments, outputFile);

      // Execute FFmpeg task
      process = Runtime.getRuntime().exec(execCommand);

      // Begin logging, if enabled
      if (loggingEnabled) {
        logAdapter = new FFmpegLogAdapter(process.getErrorStream(), getLogFile(dataDir));
        logAdapter.start();
      }

      // Allow the process to finish executing
      process.waitFor();
      process.destroy();

    } catch (InterruptedException e) {
      if (process != null) {
        Log.i(LOG_TAG, String.format("Streaming task with PID: %s interrupted", process.pid()));
      }
    } finally {
      // If logging active, finish it
      if (logAdapter != null) {
        logAdapter.join();
      }
    }
  }

  /**
   * Forcefully halt execution of this task
   *
   * @return True/false if the task was successfully killed
   */
  public boolean kill() {

    // Ensure process exists
    if (process != null) {
      // Ensure process is dead
      return !(process.destroyForcibly().isAlive());
    }
    return true;
  }

  /**
   * Create a reference to the log file location; inside streaming directory, with the format:
   * ffmpeg-yyyy-MM-dd_hh-mm-ss.log
   *
   * @param dataDir Path to streaming directory
   * @return The log file location
   */
  private File getLogFile(@NotNull final Path dataDir) {

    // Create log file name from timestamp
    final String timestamp = LocalDateTime.now().format(LOGFILE_TIMESTAMP_FORMATTER);
    final String logFilename = String.format("ffmpeg-%s.log", timestamp);

    // Create file reference in working directory
    return new File(dataDir.toFile(), logFilename);
  }
}
