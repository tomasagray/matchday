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

package self.me.matchday.util.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.VideoFileService;
import self.me.matchday.model.video.VideoFile;

import java.net.URL;
import java.sql.Timestamp;
import java.util.Optional;

@Aspect
@Component
public class VideoFileServiceLog {

  private static final Logger logger = LogManager.getLogger(VideoFileService.class);

  @Around("execution(* self.me.matchday.api.service.VideoFileService.refreshVideoFile(..))")
  public Object logRefreshVideoFile(@NotNull ProceedingJoinPoint joinPoint) throws Throwable {

    final VideoFile arg = (VideoFile) joinPoint.getArgs()[0];
    final Timestamp initRefresh = arg.getLastRefreshed();
    logger.info("Determining if VideoFile: {} should be refreshed...", arg);

    final VideoFile result = (VideoFile) joinPoint.proceed();
    final Timestamp lastRefreshed = result.getLastRefreshed();
    if (!initRefresh.equals(lastRefreshed)) {
      logger.info("VideoFile: {} was refreshed at: {}", result, lastRefreshed);
    } else {
      logger.info("VideoFile: {} was NOT refreshed", result);
    }
    return result;
  }

  @Around("execution(* self.me.matchday.api.service.VideoFileService.doVideoFileRefresh(..))")
  public Object logDoRefreshTask(@NotNull ProceedingJoinPoint jp) throws Throwable {

    final VideoFile videoFile = (VideoFile) jp.getArgs()[0];
    logger.info("Refreshing data for VideoFile: {}", videoFile);
    return jp.proceed();
  }

  @SuppressWarnings("unchecked cast")
  @Around("execution(* self.me.matchday.api.service.FileServerService.getDownloadUrl(..))")
  public Object logVideoFileInternalUrlRefresh(@NotNull ProceedingJoinPoint jp) throws Throwable {

    final URL externalUrl = (URL) jp.getArgs()[0];
    logger.info("Attempting to get internal URL for external URL: {}", externalUrl);
    final Optional<URL> result = (Optional<URL>) jp.proceed();
    if (result.isPresent()) {
      logger.info("Successfully updated internal URL to: {}", result.get());
    } else {
      logger.error("Could not update internal URL from external URL: {}", externalUrl);
    }
    return result;
  }
}
