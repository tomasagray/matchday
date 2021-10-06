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
import self.me.matchday.model.video.VideoFile;
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
public class VideoFileService {

  private static final String LOG_TAG = "VideoFileService";
  private static final int THREAD_POOL_SIZE = 12;

  // Already refreshing VideoFiles
  private final List<VideoFile> lockedVideoFiles = new ArrayList<>();
  // Services
  private final ExecutorService executorService;
  private final FileServerService fileServerService;
  private final FFmpegPlugin ffmpegPlugin;

  @Autowired
  public VideoFileService(
      final FileServerService fileServerService,
      final FFmpegPlugin ffmpegPlugin,
      final VideoFileSelectorService videoFileSelectorService) {

    this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    this.fileServerService = fileServerService;
    this.ffmpegPlugin = ffmpegPlugin;
  }

  /**
   * Add an VideoFile refresh task to the job queue
   *
   * @param videoFile The VideoFile to be refreshed
   * @param fetchMetadata Whether to pre-fetch file metadata
   * @return A Future representing the refreshed file
   */
  public VideoFile refreshVideoFile(@NotNull final VideoFile videoFile, final boolean fetchMetadata)
      throws ExecutionException, InterruptedException {

    if (!shouldRefreshData(videoFile)) {
      Log.i(LOG_TAG, "VideoFile is already fresh: " + videoFile);
      return videoFile;
    }
    // Skip locked files & already fresh data
    else if (lockedVideoFiles.contains(videoFile)) {
      final String message =
          String.format(
              "Refresh request denied for VideoFile: %s; file is locked (already being refreshed?)",
              videoFile);
      throw new HttpStatusCodeException(HttpStatus.TOO_MANY_REQUESTS, message) {
        @Override
        public @NotNull HttpStatus getStatusCode() {
          return HttpStatus.TOO_MANY_REQUESTS;
        }
      };
    }

    // Lock VideoFile
    lockedVideoFiles.add(videoFile);
    // Send each link which needs to be updated to execute in its own thread
    Log.i(LOG_TAG, "Refreshing remote data for VideoFile: " + videoFile);
    final VideoFileRefreshTask refreshTask =
        VideoFileRefreshTask.builder()
            .fileServerService(fileServerService)
            .ffmpegPlugin(ffmpegPlugin)
            .videoFile(videoFile)
            .fetchMetadata(fetchMetadata)
            .build();

    final VideoFile refreshedVideoFile = executorService.submit(refreshTask).get();
    // Unlock file & return
    lockedVideoFiles.remove(videoFile);
    return refreshedVideoFile;
  }

  /**
   * Determine whether the data for the file should be refreshed.
   *
   * @param videoFile The VideoFile with possibly stale data.
   * @return True/false
   */
  private boolean shouldRefreshData(@NotNull VideoFile videoFile) {

    // Last time this file's data refreshed
    final Instant lastRefresh = videoFile.getLastRefreshed().toInstant();
    // Time since refresh
    final Duration sinceRefresh = Duration.between(lastRefresh, Instant.now());
    // Get recommended refresh rate
    final Duration refreshRate =
        fileServerService.getFileServerRefreshRate(videoFile.getExternalUrl());
    return sinceRefresh.toMillis() > refreshRate.toMillis();
  }

  /**
   * Updates the internal (download) URL of an VideoFile, as well as the metadata, if it is null.
   * Saves updated VideoFiles to database.
   */
  @Builder
  static class VideoFileRefreshTask implements Callable<VideoFile> {
    // todo - refactor as @Async method
    private final FileServerService fileServerService;
    private final FFmpegPlugin ffmpegPlugin;
    private final VideoFile videoFile;
    private final boolean fetchMetadata;

    public VideoFileRefreshTask(
        @NotNull final FileServerService fileServerService,
        @NotNull final FFmpegPlugin ffmpegPlugin,
        @NotNull final VideoFile videoFile,
        final boolean fetchMetadata) {

      this.fileServerService = fileServerService;
      this.ffmpegPlugin = ffmpegPlugin;
      this.videoFile = videoFile;
      this.fetchMetadata = fetchMetadata;
    }

    /**
     * Gets missing or expired VideoFile data and returns a new, complete VideoFile
     *
     * @return A fully-loaded VideoFile
     */
    @SneakyThrows
    @Override
    public VideoFile call() {

      Log.i(LOG_TAG, "Refreshing data for VideoFile: " + videoFile);
      // Fetch remote internal (download) URL
      final Optional<URL> downloadUrl =
          fileServerService.getDownloadUrl(videoFile.getExternalUrl());
      if (downloadUrl.isPresent()) {
        // Update remote (internal) URL
        Log.i(
            LOG_TAG, String.format("Successfully updated remote URL for VideoFile: %s", videoFile));
        videoFile.setInternalUrl(downloadUrl.get());
        // Update metadata
        if (fetchMetadata) {
          setVideoFileMetadata();
        }
        // Update last refresh
        videoFile.setLastRefreshed(Timestamp.from(Instant.now()));
      } else {
        throw new IOException(
            String.format("Could not get remote URL for VideoFile: %s", videoFile));
      }
      // Return updated VideoFile
      return videoFile;
    }

    /**
     * Retrieves video metadata for the VideoFile associated with this task.
     *
     * @throws IOException If there is an error reading data
     */
    private void setVideoFileMetadata() throws Exception {

      // Update ONLY if metadata is null
      if (videoFile.getMetadata() == null) {
        final URI videoFileUri = videoFile.getInternalUrl().toURI();
        final FFmpegMetadata ffmpegMetadata = ffmpegPlugin.readFileMetadata(videoFileUri);
        // Ensure metadata successfully updated
        if (ffmpegMetadata != null) {
          videoFile.setMetadata(ffmpegMetadata);
        }
      }
    }
  }
}
