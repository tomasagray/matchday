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

package self.me.matchday.api.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.plugin.io.ffmpeg.FFmpegMetadata;
import self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import self.me.matchday.util.Log;

import javax.transaction.Transactional;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

@Service
@Transactional
public class EventFileService {

  private static final String LOG_TAG = "EventFileService";

  private static final int THREAD_POOL_SIZE = 12;
  // Services
  private final ExecutorService executorService;
  private final FileServerService fileServerService;
  private final FFmpegPlugin ffmpegPlugin;

  @Autowired
  public EventFileService(final FileServerService fileServerService,
      final FFmpegPlugin ffmpegPlugin) {

    this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    this.fileServerService = fileServerService;
    this.ffmpegPlugin = ffmpegPlugin;
  }

  /**
   * Sends each EventFile from a File Source to its own thread to fetch missing or stale file data.
   * Updates EventFiles in the local database.
   *
   * @param eventFileSource The File Source to be refreshed.
   */
  public void refreshEventFileData(@NotNull final EventFileSource eventFileSource,
      final boolean fetchMetadata) {

    Log.i(LOG_TAG, "Refreshing remote data for file source: " + eventFileSource);

    // Remote files container
    final Set<Future<EventFile>> futureEventFiles = new LinkedHashSet<>();
    // Send each link which needs to be updated to execute in its own thread
    eventFileSource
        .getEventFiles()
        .stream()
        .filter(this::shouldRefreshData)
        .forEach(
            eventFile ->
                futureEventFiles.add(
                    executorService.submit(
                        new EventFileRefreshTask(fileServerService, ffmpegPlugin,
                            eventFile, fetchMetadata)))
        );

    // Retrieve results of remote fetch operation
    futureEventFiles.forEach(
        eventFileFuture -> {
          try {
            final EventFile eventFile = eventFileFuture.get();
            // Update managed version
            mergeEventFile(eventFileSource, eventFile);
          } catch (InterruptedException | ExecutionException e) {
            Log.d(LOG_TAG, "Could not fetch remote file " + eventFileFuture, e);
          }
        });
  }

  /**
   * Determine whether the data for the file should be refreshed.
   *
   * @param eventFile The EventFile with possibly stale data.
   * @return True/false
   */
  private boolean shouldRefreshData(@NotNull EventFile eventFile) {

    // Last time this file's data refreshed
    final Instant lastRefresh = eventFile.getLastRefreshed().toInstant();
    // Time since refresh
    final Duration sinceRefresh = Duration.between(lastRefresh, Instant.now());
    // Get recommended refresh rate
    final Duration refreshRate =
        fileServerService.getFileServerRefreshRate(eventFile.getExternalUrl());
    return sinceRefresh.toMillis() > refreshRate.toMillis();
  }

  /**
   * Update the managed EventFile with data retrieved from the remote task.
   *
   * @param eventFileSource The EvenFileSource with the managed version of the EventFile
   * @param eventFile       The updated (refreshed) EventFile data
   */
  private void mergeEventFile(@NotNull EventFileSource eventFileSource,
      @NotNull EventFile eventFile) {

    // Find the EventFile that needs updating
    eventFileSource.getEventFiles().forEach(ef -> {
      if (eventFile.getEventFileId().equals(ef.getEventFileId())) {
        // Copy data
        ef.setInternalUrl(eventFile.getInternalUrl());
        ef.setMetadata(eventFile.getMetadata());
        ef.setLastRefreshed(eventFile.getLastRefreshed());
      }
    });
  }

  /**
   * Updates the internal (download) URL of an EventFile, as well as the metadata, if it is null.
   * Saves updated EventFiles to database.
   */
  private static class EventFileRefreshTask implements Callable<EventFile> {

    private final FileServerService fileServerService;
    private final FFmpegPlugin ffmpegPlugin;
    private final EventFile eventFile;
    private final boolean fetchMetadata;

    public EventFileRefreshTask(@NotNull final FileServerService fileServerService,
        @NotNull final FFmpegPlugin ffmpegPlugin,
        @NotNull final EventFile eventFile, final boolean fetchMetadata) {

      this.fileServerService = fileServerService;
      this.ffmpegPlugin = ffmpegPlugin;
      this.eventFile = eventFile;
      this.fetchMetadata = fetchMetadata;
    }

    /**
     * Gets missing or expired EventFile data and returns a new, complete EventFile
     *
     * @return A fully-loaded EventFile
     */
    @Override
    public EventFile call() {

      Log.i(LOG_TAG, "Refreshing data for EventFile: " + eventFile);
      try {
        // Fetch remote internal (download) URL
        final Optional<URL> downloadUrl =
            fileServerService.getDownloadUrl(eventFile.getExternalUrl());

        if (downloadUrl.isPresent()) {
          // Update remote (internal) URL
          Log.i(LOG_TAG,
              String.format("Successfully updated remote URL for EventFile: %s", eventFile));
          eventFile.setInternalUrl(downloadUrl.get());
          // Update metadata
          if (fetchMetadata) {
            setEventFileMetadata();
          }
          // Update last refresh
          eventFile.setLastRefreshed(Timestamp.from(Instant.now()));

        } else {
          throw new
              IOException(String.format("Could not get remote URL for EventFile: %s", eventFile));
        }
      } catch (Exception e) {
        Log.e(LOG_TAG,
            String.format("Could not refresh remote data for EventFile: %s", eventFile),
            e);
      }
      // Return updated EventFile
      return eventFile;
    }

    /**
     * Retrieves video metadata for the EventFile associated with this task.
     *
     * @throws IOException If there is an error reading data
     */
    private void setEventFileMetadata() throws Exception {

      // Update ONLY if metadata is null
      if (eventFile.getMetadata() == null) {
        final URI eventFileUri = eventFile.getInternalUrl().toURI();
        final FFmpegMetadata ffmpegMetadata =
            ffmpegPlugin.readFileMetadata(eventFileUri);
        // Ensure metadata successfully updated
        if (ffmpegMetadata != null) {
          eventFile.setMetadata(ffmpegMetadata);
        }
      }
    }
  }
}
