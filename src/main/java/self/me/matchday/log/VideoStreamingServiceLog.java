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

package self.me.matchday.log;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.video.VideoStreamingService;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoStreamLocator;

@Aspect
public class VideoStreamingServiceLog {

  private static final Logger logger = LogManager.getLogger(VideoStreamingService.class);

  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamingService.fetchVideoFileSources(..))")
  public Object logFetchVideoFileSources(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final Object eventId = jp.getArgs()[0];
    logger.info("Fetching VideoFileSources for Event: {}", eventId);
    final Object result = jp.proceed();
    logger.debug("Retrieved VideoFileSources: {} for Event: {}", result, eventId);
    return result;
  }

  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamingService.getVideoStreamPlaylist(..))")
  public Object logGetBestVideoStreamPlaylist(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info(
        "Getting optimal VideoStreamPlaylist for Event: {}",
        jp.getArgs()[0]);
    final Object result = jp.proceed();
    logger.debug("Returned playlist: {}", result);
    return result;
  }

  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamingService.getVideoStreamPlaylist(..))")
  public Object logGetVideoStreamPlaylist(@NotNull ProceedingJoinPoint jp) throws Throwable {

    logger.info(
        "Getting VideoStreamPlaylist for Event: {}, File Source: {}",
        jp.getArgs()[0],
        jp.getArgs()[1]);
    final Object result = jp.proceed();
    logger.info("Found: {}", result);
    return result;
  }

  @SuppressWarnings("unchecked cast")
  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamingService.readPlaylistFile(..))")
  public Object logReadPlaylistFile(@NotNull ProceedingJoinPoint jp) throws Throwable {

    logger.info("Attempting to read playlist file for Locator ID: {}", jp.getArgs()[0]);
    final Optional<String> result = (Optional<String>) jp.proceed();
    final int byteCount = result.map(s -> s.getBytes(StandardCharsets.UTF_8).length).orElse(0);
    logger.info("Read {} bytes of playlist file", byteCount);
    return result;
  }

  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamingService.readLocatorPlaylist(..))")
  public Object logReadPlaylistDataForLocator(@NotNull ProceedingJoinPoint jp) throws Throwable {
    VideoStreamLocator locator = (VideoStreamLocator) jp.getArgs()[0];
    Long streamLocatorId = locator.getStreamLocatorId();
    logger.info("Reading playlist data from disk for VideoStreamLocator: {}", streamLocatorId);
    String result = (String) jp.proceed();
    long byteCount = result != null ? result.getBytes(StandardCharsets.UTF_8).length : 0;
    logger.info(
        "Read: {} bytes for playlist of VideoStreamLocator: {}", byteCount, streamLocatorId);
    return result;
  }

  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamingService.getVideoSegmentResource(..))")
  public Object logGetVideoSegmentResource(@NotNull ProceedingJoinPoint jp) throws Throwable {

    logger.info(
        "Reading video segment resource for Locator ID: {}, Segment: {}",
        jp.getArgs()[0],
        jp.getArgs()[1]);
    final Object result = jp.proceed();
    logger.info("Read: {} for Locator: {}", result, jp.getArgs()[0]);
    return result;
  }

  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamingService.killAllStreamingTasks())")
  public Object logKillAllStreamingTasks(@NotNull ProceedingJoinPoint jp) throws Throwable {

    logger.info("Attempting to kill all streaming tasks...");
    final Object result = jp.proceed();
    logger.info("Killed: {} tasks", result);
    return result;
  }

  @Before(
      "execution(* self.me.matchday.api.service.video.VideoStreamingService.killStreamingFor(..))")
  public void logKillStreamingFor(@NotNull JoinPoint jp) {
    logger.info("Attempting to kill streams for Video File Source: {}", jp.getArgs()[0]);
  }

  @Before(
      "execution(* self.me.matchday.api.service.video.VideoStreamingService.deleteVideoData(..))")
  public void logDeleteVideoData(@NotNull JoinPoint jp) {
    logger.info("Attempting to delete video data from disk for: {}", jp.getArgs()[0]);
  }

  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamingService.renderPlaylist(..))")
  public Object logRenderPlaylist(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final Object[] args = jp.getArgs();
    logger.info(
        "Rendering playlist for Event: {}, File Source: {}, locator playlist: {}",
        args[0],
        args[1],
        args[2]);
    final Object result = jp.proceed();
    logger.info("Rendered playlist: {} for File Source: {}", result, args[1]);
    return result;
  }

  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamingService.createVideoStream(..))")
  public Object logCreatePlaylist(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final VideoFileSource fileSource = (VideoFileSource) jp.getArgs()[0];
    final UUID fileSrcId = fileSource.getFileSrcId();
    logger.info("Creating playlist for VideoFileSource: {}", fileSrcId);
    final Object result = jp.proceed();
    logger.info("Created playlist: {} for VideoFileSource: {}", result, fileSrcId);
    return result;
  }

  @Before("execution(* self.me.matchday.api.controller.VideoStreamingController.handle*(..))")
  public void logStreamingError(@NotNull JoinPoint jp) {
    final Throwable e = (Throwable) jp.getArgs()[0];
    final String error =
        Arrays.stream(e.getStackTrace())
          .map(StackTraceElement::toString)
          .collect(Collectors.joining("\n"));
    logger.debug(error);
  }
}
