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

package self.me.matchday.api.service;

import lombok.Builder;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import self.me.matchday.model.EventFile;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Transactional
public class EventFileService {

  private static final String LOG_TAG = "EventFileService";
  private static final int THREAD_POOL_SIZE = 12;

  // Already refreshing EventFiles
  private final List<EventFile> lockedEventFiles = new ArrayList<>();
  // Services
  private final ExecutorService executorService;
  private final FileServerService fileServerService;
  private final FFmpegPlugin ffmpegPlugin;

  @Autowired
  public EventFileService(
      final FileServerService fileServerService,
      final FFmpegPlugin ffmpegPlugin,
      final EventFileSelectorService eventFileSelectorService) {

    this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    this.fileServerService = fileServerService;
    this.ffmpegPlugin = ffmpegPlugin;
  }

  /**
   * Add an EventFile refresh task to the job queue
   *
   * @param eventFile The EventFile to be refreshed
   * @param fetchMetadata Whether to pre-fetch file metadata
   * @return A Future representing the refreshed file
   */
  public EventFile refreshEventFile(@NotNull final EventFile eventFile, final boolean fetchMetadata)
      throws ExecutionException, InterruptedException {

    if (!shouldRefreshData(eventFile)) {
      Log.i(LOG_TAG, "EventFile is already fresh: " + eventFile);
      return eventFile;
    }
    // Skip locked files & already fresh data
    else if (lockedEventFiles.contains(eventFile)) {
      final String message =
          String.format(
              "Refresh request denied for EventFile: %s; file is locked (already being refreshed?)",
              eventFile);
      throw new HttpStatusCodeException(HttpStatus.TOO_MANY_REQUESTS, message) {
        @Override
        public @NotNull HttpStatus getStatusCode() {
          return HttpStatus.TOO_MANY_REQUESTS;
        }
      };
    }

    // Lock EventFile
    lockedEventFiles.add(eventFile);
    // Send each link which needs to be updated to execute in its own thread
    Log.i(LOG_TAG, "Refreshing remote data for EventFile: " + eventFile);
    final EventFileRefreshTask refreshTask =
        EventFileRefreshTask.builder()
            .fileServerService(fileServerService)
            .ffmpegPlugin(ffmpegPlugin)
            .eventFile(eventFile)
            .fetchMetadata(fetchMetadata)
            .build();

    final EventFile refreshedEventFile = executorService.submit(refreshTask).get();
    // Unlock file & return
    lockedEventFiles.remove(eventFile);
    return refreshedEventFile;
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
   * Updates the internal (download) URL of an EventFile, as well as the metadata, if it is null.
   * Saves updated EventFiles to database.
   */
  @Builder
  static class EventFileRefreshTask implements Callable<EventFile> {
    // todo - refactor as @Async method
    private final FileServerService fileServerService;
    private final FFmpegPlugin ffmpegPlugin;
    private final EventFile eventFile;
    private final boolean fetchMetadata;

    public EventFileRefreshTask(
        @NotNull final FileServerService fileServerService,
        @NotNull final FFmpegPlugin ffmpegPlugin,
        @NotNull final EventFile eventFile,
        final boolean fetchMetadata) {

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
    @SneakyThrows
    @Override
    public EventFile call() {

      Log.i(LOG_TAG, "Refreshing data for EventFile: " + eventFile);
      // Fetch remote internal (download) URL
      final Optional<URL> downloadUrl =
          fileServerService.getDownloadUrl(eventFile.getExternalUrl());
      if (downloadUrl.isPresent()) {
        // Update remote (internal) URL
        Log.i(
            LOG_TAG, String.format("Successfully updated remote URL for EventFile: %s", eventFile));
        eventFile.setInternalUrl(downloadUrl.get());
        // Update metadata
        if (fetchMetadata) {
          setEventFileMetadata();
        }
        // Update last refresh
        eventFile.setLastRefreshed(Timestamp.from(Instant.now()));
      } else {
        throw new IOException(
            String.format("Could not get remote URL for EventFile: %s", eventFile));
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
        final FFmpegMetadata ffmpegMetadata = ffmpegPlugin.readFileMetadata(eventFileUri);
        // Ensure metadata successfully updated
        if (ffmpegMetadata != null) {
          eventFile.setMetadata(ffmpegMetadata);
        }
      }
    }
  }
}
