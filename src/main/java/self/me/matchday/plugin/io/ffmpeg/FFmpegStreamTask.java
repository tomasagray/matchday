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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class FFmpegStreamTask extends Thread {

  private static final String LOG_TAG = "FFmpegStreamTask";
  private static final DateTimeFormatter LOGFILE_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_hh-mm-ss");

  protected String command;
  protected List<String> transcodeArgs;
  protected Path playlistPath;
  protected Path dataDir;
  protected boolean loggingEnabled;
  protected Process process;
  protected FFmpegLogAdapter logAdapter;

  @SneakyThrows
  @Override
  public void run() {

    try {
      // Perform necessary prerequisite steps
      prepareStream();

      // Get formatted exec command
      String execCommand = getExecCommand();
      Log.i(LOG_TAG, "Executing shell command:\n" + execCommand);

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
  public final boolean kill() {

    // Stop logging
    if (logAdapter != null) {
      logAdapter.interrupt();
    }
    // Ensure process exists
    if (process != null) {
      // Ensure process is dead
      return !(process.destroyForcibly().isAlive());
    }
    return false;
  }

  /**
   * Get a formatted executable command (system CLI)
   *
   * @return The execution command
   */
  abstract String getExecCommand();

  /**
   * Perform necessary preliminary setup tasks
   *
   * @throws IOException If there are any problems with stream preparation
   */
  abstract void prepareStream() throws IOException;

  /**
   * Returns a formatted String containing the input portion of the FFMPEG command
   *
   * @return The input portion of the FFMPEG command
   */
  abstract String getInputString();

  /**
   * Create a reference to the log file location; inside streaming directory, with the format:
   * ffmpeg-yyyy-MM-dd_hh-mm-ss.log
   *
   * @param dataDir Path to streaming directory
   * @return The log file location
   */
  private @NotNull File getLogFile(@NotNull final Path dataDir) {

    // Create log file name from timestamp
    final String timestamp = LocalDateTime.now().format(LOGFILE_TIMESTAMP_FORMATTER);
    final String logFilename = String.format("ffmpeg-%s.log", timestamp);

    // Create file reference in working directory
    return new File(dataDir.toFile(), logFilename);
  }
}
