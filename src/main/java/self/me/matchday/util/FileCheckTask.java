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

package self.me.matchday.util;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Checks whether a given file exists yet. Will timeout after a specified period, or 45 seconds by
 * default. Runs in its own thread.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FileCheckTask extends Thread {

  private static final String LOG_TAG = "FileCheckTask";
  // Default parameters
  public static final int DEFAULT_DELAY = 500;
  private static final Duration MAX_TIMEOUT = Duration.ofSeconds(45);

  static class TimedFileCheckTask extends TimerTask {

    private final FileCheckTask checkTask;

    public TimedFileCheckTask(@NotNull final FileCheckTask checkTask) {
      this.checkTask = checkTask;
    }

    @Override
    public void run() {

      // Determine if the given file exists
      final File playlistFile = checkTask.getFile();
      checkTask.setFileFound(playlistFile.exists());
    }
  }

  private final File file;
  private final long checkInterval;
  private final Duration timeout;
  private Duration executionTime;
  private boolean fileFound;

  public FileCheckTask(@NotNull final File file, final long checkInterval) {
    this(file, checkInterval, MAX_TIMEOUT);
  }

  public FileCheckTask(
      @NotNull final File file, final long checkInterval, @NotNull final Duration timeout) {

    this.file = file;
    this.checkInterval = checkInterval;
    this.timeout = timeout;
  }

  @Override
  public void run() {

    // Set timeout counter
    Instant taskStarted = Instant.now();
    Log.i(LOG_TAG, String.format("Task started at: %s for file:\n%s", taskStarted, file));

    // Check for file at regular intervals
    Timer timer = new Timer();
    timer.schedule(new TimedFileCheckTask(this), DEFAULT_DELAY, checkInterval);

    // Run until timeout
    while (Duration.between(taskStarted, Instant.now()).compareTo(timeout) <= 0) {
      // Quit when the stream has begun
      if (fileFound) {
        this.executionTime = Duration.between(taskStarted, Instant.now());
        return;
      }
    }
  }
}
