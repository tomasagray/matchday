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

import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;
import self.me.matchday.util.Log;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@NoArgsConstructor
public class VideoResourceInterceptor implements HandlerInterceptor {

  private static final String LOG_TAG = "Interceptor";
  private static final Long PROCESS_DELAY = 1_000L;
  private static final Pattern DATA_PATH_PATTERN =
      Pattern.compile("[\\w\\\\/]+(\\w{32})[\\\\/]([\\w-]{32,36})[\\\\/][\\w]+.m3u8");

  @Autowired
  VideoStreamingService videoStreamingService;
  private String fileStorageLocation;

  public VideoResourceInterceptor(@NotNull final String fileStorageLocation) {
    this.fileStorageLocation = fileStorageLocation;
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
  public boolean preHandle(@NotNull final HttpServletRequest request,
      @NotNull final HttpServletResponse response, @NotNull final Object handler) {

    final String servletPath = request.getServletPath();

    try {
      // Validate request path
      final Matcher pathMatcher = DATA_PATH_PATTERN.matcher(servletPath);
      if (pathMatcher.find()) {

        // Get playlist file path
        final File playlist = Paths.get(fileStorageLocation, servletPath).toFile();
        // Determine if playlist exists
        if (!playlist.exists()) {
          // Extract parameters from path
          final String eventId = pathMatcher.group(1);
          final String fileSrcId = pathMatcher.group(2);
          // Validate parameters
          if (eventId != null && !("".equals(eventId)) &&
              fileSrcId != null && !("".equals(fileSrcId))) {

            // Stream video files to local disk
            videoStreamingService.createVideoStream(eventId, fileSrcId);
            // Ensure playlist creation has begun
            Thread.sleep(PROCESS_DELAY);
          }
        }
      }
    } catch (Exception e) {
      Log.e(LOG_TAG,
          String.format("Error streaming video files; request URI: %s", request.getRequestURI()),
          e);
    }
    // Continue processing request
    return true;
  }
}
