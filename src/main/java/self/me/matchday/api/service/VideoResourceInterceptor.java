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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;
import self.me.matchday.model.VideoStreamPlaylistLocator;
import self.me.matchday.util.FileCheckTask;
import self.me.matchday.util.Log;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.Path;
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
  private final PlaylistLocatorService playlistLocatorService;

  public VideoResourceInterceptor(
      @Autowired final VideoStreamingService videoStreamingService,
      @Autowired final PlaylistLocatorService playlistLocatorService) {

    this.videoStreamingService = videoStreamingService;
    this.playlistLocatorService = playlistLocatorService;
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
      @NotNull final Object handler) {

    final String servletPath = request.getServletPath();
    Log.i(LOG_TAG, LOG_TAG + " caught request at path: " + servletPath);

    try {
      // Validate request path
      final Matcher pathMatcher = urlPattern.matcher(servletPath);
      if (pathMatcher.find()) {

        // Extract parameters from path
        final String eventId = pathMatcher.group(1);
        final String fileSrcId = pathMatcher.group(2);

        // Get playlist locator
        final Optional<VideoStreamPlaylistLocator> locatorOptional =
            playlistLocatorService.getPlaylistLocator(eventId, fileSrcId);
        if (locatorOptional.isPresent()) {
          // video has already begun streaming
          Log.i(
              LOG_TAG,
              String.format(
                  "Found playlist locator: %s; video has begun streaming", locatorOptional.get()));
          return true;
        }

        // Stream video files to local disk
        Log.i(LOG_TAG, String.format("Creating video stream for event: %s, file source: %s", eventId, fileSrcId));
        final Optional<VideoStreamPlaylistLocator> playlistOptional =
            videoStreamingService.createVideoStream(eventId, fileSrcId);

        if (playlistOptional.isPresent()) {
          VideoStreamPlaylistLocator playlistLocator = playlistOptional.get();
          final Path playlistPath = playlistLocator.getPlaylistPath().toAbsolutePath();
          Log.i(LOG_TAG, "Created video stream at: " + playlistPath);

          // Ensure playlist creation has begun
          Log.i(LOG_TAG, "Waiting for stream head start...");
          final FileCheckTask fileCheckTask = new FileCheckTask(playlistPath.toFile(), FILE_CHECK_DELAY);
          // Start checking
          fileCheckTask.start();
          // Wait until task finishes or times out
          fileCheckTask.join();
          final boolean fileFound = fileCheckTask.isFileFound();
          Log.i(LOG_TAG, "Playlist file found? " + fileFound);
          // If file not found, tell Spring to stop processing request
          return fileFound;

        } else {
          Log.d(
              LOG_TAG,
              String.format(
                  "Could not create playlist for Event: %s, File Source: %s", eventId, fileSrcId));
        }

      } else {
        Log.e(LOG_TAG, "Could not extract necessary data from servlet path: " + servletPath);
        return false;
      }
    } catch (Throwable e) {
      Log.e(
          LOG_TAG,
          String.format("Error streaming video files; request URI: %s", request.getRequestURI()),
          e);
      return false;
    }
    // Continue processing request
    return true;
  }
}
