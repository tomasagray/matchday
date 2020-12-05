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

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Log;

import java.io.*;

/** Read FFmpegTask output and write to a file on disk */
class FFmpegLogAdapter extends Thread {

  private static final String LOG_TAG = "FFmpegLogAdapter";

  private final InputStream inputStream;
  private final File logFile;

  FFmpegLogAdapter(@NotNull final InputStream inputStream, @NotNull final File logFile) {

    this.inputStream = inputStream;
    this.logFile = logFile;
  }

  @SneakyThrows
  @Override
  public void run() {

    // Ensure FFmpeg has time to start
    Thread.sleep(1_000);
    Log.i(LOG_TAG, "Beginning log output to: " + logFile.getAbsolutePath());

    // Write FFmpeg output (InputStream) to log file
    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        final FileWriter logFileWriter = new FileWriter(logFile)) {

      String line;
      while ((line = reader.readLine()) != null) {
        logFileWriter.write(line + "\n");
      }
      // Ensure all data written to file
      logFileWriter.flush();

    } catch (IOException e) {
      Log.e(LOG_TAG, "Error writing FFmpeg log file", e);
    }
  }
}
