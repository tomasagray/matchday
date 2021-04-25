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

package self.me.matchday.api.service.video;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;
import self.me.matchday.model.M3UPlaylist;
import self.me.matchday.model.VideoStreamLocator;
import self.me.matchday.model.VideoStreamPlaylist;
import self.me.matchday.util.FileCheckTask;
import self.me.matchday.util.Log;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VideoResourceInterceptor implements HandlerInterceptor {

  private static final String LOG_TAG = "VideoResourceInterceptor";
  private static final long FILE_CHECK_DELAY = 250;

  @Value("${video-resources.video-stream-path-pattern}")
  private Pattern urlPattern;

  private final VideoStreamingService videoStreamingService;

  public VideoResourceInterceptor(@Autowired final VideoStreamingService videoStreamingService) {

    this.videoStreamingService = videoStreamingService;
  }

  /**
   * Intercept the request for a playlist file. If it does not exist, create it. If a playlist is
   * created, this method will pause execution for a predetermined length of time to ensure the
   * external process has time to begin transcoding before continuing processing of the request.
   *
   * @param request The HTTP request
   * @param response The HTTP response
   * @param handler Response handler; not used
   * @return True if the playlist was successfully created; otherwise false
   */
  @Override
  @Transactional
  public boolean preHandle(
      @NotNull final HttpServletRequest request,
      @NotNull final HttpServletResponse response,
      @NotNull final Object handler)
      throws Exception {

    final String servletPath = request.getServletPath();
    Log.i(LOG_TAG, LOG_TAG + " caught request at path: " + servletPath);

    // Validate request path
    final Matcher pathMatcher = urlPattern.matcher(servletPath);
    if (pathMatcher.find()) {

      // Extract parameters from path
      final String eventId = pathMatcher.group(1);
      final String fileSrcId = pathMatcher.group(2);

      // Is there already a stream playlist?
      if (streamingHasBegun(eventId, fileSrcId)) {
        Log.i(
            LOG_TAG,
            String.format(
                "Video for Event: %s, File Source: %s has begun streaming", eventId, fileSrcId));
        return true;
      }

      // Stream video files to local disk
      Log.i(
          LOG_TAG,
          String.format(
              "Creating video stream for event: %s, file source: %s", eventId, fileSrcId));
      final Optional<VideoStreamPlaylist> playlistOptional =
          videoStreamingService.createVideoStream(eventId, fileSrcId);
      if (playlistOptional.isPresent()) {
        final VideoStreamPlaylist streamPlaylist = playlistOptional.get();
        final Path playlistPath = getStreamingPath(streamPlaylist);
        Log.i(LOG_TAG, "Created video stream at: " + playlistPath);

        // Wait until FFMPEG has started writing data to local disk
        final boolean fileFound = waitForStreamToStart(playlistPath);
        // Wait a little longer...
        Thread.sleep(3 * 1_000); // todo - figure out a better way to handle delay
        // If file not found, tell Spring to stop processing request
        return fileFound;

      } else {
        final String message =
            String.format(
                "Could not create stream for Event: %s, File Source: %s", eventId, fileSrcId);
        throw new RuntimeException(message);
      }
    } else {
      final String message = "Could not extract necessary data from servlet path: " + servletPath;
      throw new IllegalArgumentException(message);
    }
  }

  private @NotNull Path getStreamingPath(@NotNull final VideoStreamPlaylist streamPlaylist) {

    final VideoStreamLocator firstStreamLocator = streamPlaylist.getStreamLocators().get(0);
    return firstStreamLocator.getPlaylistPath().toAbsolutePath();
  }

  private boolean waitForStreamToStart(Path playlistPath) throws InterruptedException {

    // Ensure playlist creation has begun
    Log.i(LOG_TAG, "Waiting for stream head start...");
    final FileCheckTask fileCheckTask = new FileCheckTask(playlistPath.toFile(), FILE_CHECK_DELAY);
    // Start checking
    fileCheckTask.start();
    // Wait until task finishes or times out
    fileCheckTask.join();
    final boolean fileFound = fileCheckTask.isFileFound();
    final Duration executionTime = fileCheckTask.getExecutionTime();
    Log.i(
        LOG_TAG,
        String.format(
            "Playlist file found? %s, Time taken for stream to start: %s seconds",
            fileFound, executionTime.getSeconds()));
    return fileFound;
  }

  private boolean streamingHasBegun(String eventId, String fileSrcId) {

    final Optional<M3UPlaylist> playlistOptional =
        videoStreamingService.getVideoStreamPlaylist(eventId, fileSrcId);
    return playlistOptional.isPresent();
  }
}
