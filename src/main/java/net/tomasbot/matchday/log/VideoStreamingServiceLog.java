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

package net.tomasbot.matchday.log;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.tomasbot.matchday.model.video.VideoFileSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;

@Aspect
public class VideoStreamingServiceLog {

  private static final Logger logger = LogManager.getLogger(VideoStreamingServiceLog.class);

  @Before(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamingService.getBestVideoStreamPlaylist(..))")
  public void logGetBestVideoStreamPlaylist(@NotNull JoinPoint jp) {
    logger.info("Getting 'best' video stream for Event: {}", jp.getArgs()[0]);
  }

  @Before(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamingService.findExistingStream(..))")
  public void logFindExistingStream(@NotNull JoinPoint jp) {
    logger.info("Finding existing stream for Event: {}", jp.getArgs()[0]);
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamingService.getVideoSegmentResource(..))")
  public Object logGetVideoSegmentResource(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.trace(
        "Reading video segment resource for Locator ID: {}, Segment: {}",
        jp.getArgs()[0],
        jp.getArgs()[1]);
    final Object result = jp.proceed();
    logger.debug("Read: {} for Locator: {}", result, jp.getArgs()[0]);
    return result;
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamingService.renderPlaylist(..))")
  public Object logRenderPlaylist(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final Object[] args = jp.getArgs();
    logger.info("Rendering playlist for Event: {}, locator playlist: {}", args[0], args[1]);
    final Object result = jp.proceed();
    logger.info("Rendered playlist: {} for File Source: {}", result, args[1]);
    return result;
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamingService.createVideoStream(..))")
  public Object logCreatePlaylist(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final VideoFileSource fileSource = (VideoFileSource) jp.getArgs()[0];
    final UUID fileSrcId = fileSource.getFileSrcId();
    logger.info("Creating playlist for VideoFileSource: {}", fileSrcId);
    final Object result = jp.proceed();
    logger.info("Created playlist: {} for VideoFileSource: {}", result, fileSrcId);
    return result;
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamingService.getOrCreateVideoStreamPlaylist(..))")
  public Object logGetVideoStreamPlaylist(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info(
        "Getting VideoStreamPlaylist for Event: {}, File Source: {}",
        jp.getArgs()[0],
        jp.getArgs()[1]);
    final Object result = jp.proceed();
    logger.info("Found: {}", result);
    return result;
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamingService.readPlaylistFile(..))")
  public Object logReadPlaylistFile(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.trace("Attempting to read playlist file for Locator ID: {}", jp.getArgs()[0]);
    final String result = (String) jp.proceed();
    final int byteCount = result.getBytes(StandardCharsets.UTF_8).length;
    logger.debug("Read {} bytes of playlist file", byteCount);
    return result;
  }

  @AfterReturning(
      value =
          "execution(* net.tomasbot.matchday.api.service.video.VideoStreamingService.getActiveStreamingTaskCount(..))",
      returning = "count")
  public void logGetActiveStreamTaskCount(@NotNull Object count) {
    logger.info("There are currently {} active video stream tasks", count);
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamingService.killAllStreamingTasks())")
  public Object logKillAllStreamingTasks(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Attempting to kill all streaming tasks...");
    final Object result = jp.proceed();
    logger.info("Killed: {} tasks", result);
    return result;
  }

  @Before(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamingService.killAllStreamsFor(..))")
  public void logKillStreamingFor(@NotNull JoinPoint jp) {
    logger.info("Attempting to kill streams for Video File Source: {}", jp.getArgs()[0]);
  }

  @Before(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamingService.killStreamFor(..))")
  public void logKillVideoFileStream(@NotNull JoinPoint jp) {
    logger.info("Attempting to kill stream for VideoFile: {}", jp.getArgs()[0]);
  }

  @Before(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamingService.deleteAllVideoData(..))")
  public void logDeleteAllVideoDataForSource(@NotNull JoinPoint jp) {
    logger.info("Attempting to delete video data from disk for: {}", jp.getArgs()[0]);
  }

  @Before(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamingService.deleteVideoData(..))")
  public void logDeleteVideoStreamForVideoFile(@NotNull JoinPoint jp) {
    logger.info("Deleting video stream data for VideoFile: {}", jp.getArgs()[0]);
  }
}
