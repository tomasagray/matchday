/*
 * Copyright (c) 2022.
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

package net.tomasbot.matchday.api.service.video;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.transaction.Transactional;
import lombok.Getter;
import net.tomasbot.ffmpeg_wrapper.metadata.FFmpegMetadata;
import net.tomasbot.matchday.api.service.FileServerPluginService;
import net.tomasbot.matchday.api.service.UrlResolverPluginService;
import net.tomasbot.matchday.db.VideoFileRepository;
import net.tomasbot.matchday.model.video.VideoFile;
import net.tomasbot.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

@Service
@Transactional
public class VideoFileService {

  // already refreshing VideoFiles
  @Getter private final List<VideoFile> lockedVideoFiles = new ArrayList<>();
  private final FFmpegPlugin ffmpegPlugin;
  private final FileServerPluginService fileServerService;
  private final UrlResolverPluginService urlResolverService;
  private final VideoFileRepository videoFileRepository;

  public VideoFileService(
      FFmpegPlugin ffmpegPlugin,
      FileServerPluginService fileServerService,
      UrlResolverPluginService urlResolverService,
      VideoFileRepository videoFileRepository) {
    this.ffmpegPlugin = ffmpegPlugin;
    this.fileServerService = fileServerService;
    this.urlResolverService = urlResolverService;
    this.videoFileRepository = videoFileRepository;
  }

  /**
   * Add an VideoFile refresh task to the job queue
   *
   * @param videoFile The VideoFile to be refreshed
   * @param fetchMetadata Whether to pre-fetch file metadata
   * @return A Future representing the refreshed file
   */
  public synchronized VideoFile refreshVideoFile(
      @NotNull final VideoFile videoFile, final boolean fetchMetadata) throws Exception {
    if (lockedVideoFiles.contains(videoFile)) {
      rejectRequest(videoFile);
    }
    try {
      if (shouldRefreshData(videoFile)) {
        lockedVideoFiles.add(videoFile);
        return doVideoFileRefresh(videoFile, fetchMetadata).get();
      }
      return videoFile;
    } finally {
      lockedVideoFiles.remove(videoFile);
    }
  }

  private void rejectRequest(@NotNull VideoFile videoFile) {
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

  @Async("VideoFileRefresher")
  public CompletableFuture<VideoFile> doVideoFileRefresh(
      @NotNull VideoFile videoFile, boolean fetchMetadata) throws Exception {
    // Fetch remote internal (download) URL
    URL resolved = getForwardedUrl(videoFile);
    Optional<URL> downloadUrl = fileServerService.getDownloadUrl(resolved);

    if (downloadUrl.isPresent()) {
      videoFile.setInternalUrl(downloadUrl.get());
      if (fetchMetadata) setVideoFileMetadata(videoFile);
      videoFile.setLastRefreshed(Timestamp.from(Instant.now()));
      videoFileRepository.save(videoFile);
    } else throw new IOException("Could not get remote URL for VideoFile: " + videoFile);

    // Return updated VideoFile
    return CompletableFuture.completedFuture(videoFile);
  }

  private @NotNull URL getForwardedUrl(@NotNull VideoFile videoFile)
      throws IOException, URISyntaxException {
    URL externalUrl = videoFile.getExternalUrl();
    URL resolved = urlResolverService.resolve(externalUrl);

    // update database if we have a new URL
    if (!resolved.toURI().equals(externalUrl.toURI())) {
      videoFile.setExternalUrl(resolved);
      videoFileRepository.save(videoFile);
    }

    return resolved;
  }

  /**
   * Retrieves video metadata for the VideoFile associated with this task.
   *
   * @throws IOException If there is an error reading data
   */
  private void setVideoFileMetadata(@NotNull VideoFile videoFile) throws Exception {
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
