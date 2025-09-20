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

import net.tomasbot.matchday.model.video.VideoFileSource;
import net.tomasbot.matchday.model.video.VideoStreamLocator;
import net.tomasbot.matchday.model.video.VideoStreamLocatorPlaylist;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;

@Aspect
public class VideoStreamManagerLog {

  private static final Logger logger = LogManager.getLogger(VideoStreamManagerLog.class);

  @Around(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamManager.createVideoStreamFrom(..))")
  public Object logCreateVideoStreamFrom(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final VideoFileSource fileSource = (VideoFileSource) jp.getArgs()[0];
    logger.info("Creating video stream from VideoFileSource: {}", fileSource.getFileSrcId());
    final Object result = jp.proceed();
    logger.info("Produced playlist: {}", result);
    return result;
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamManager.getLocalStreamFor(..))")
  public Object logGetLocalStreamFor(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final Object arg = jp.getArgs()[0];
    logger.info("Getting local stream for File Source: {}", arg);
    final Object result = jp.proceed();
    logger.info("Got local stream: {} for File Source: {}", result, arg);
    return result;
  }

  @Before(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamManager.deleteLocalStreams(..))")
  public void logDeleteLocalStream(@NotNull JoinPoint jp) {
    final VideoStreamLocatorPlaylist playlist = (VideoStreamLocatorPlaylist) jp.getArgs()[0];
    logger.info("Deleting local stream for VideoStreamLocatorPlaylist: {}", playlist.getId());
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamManager.killAllStreams())")
  public Object logKillAllStreams(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Attempting to kill all streaming tasks...");
    final int result = (int) jp.proceed();
    logger.info("Interrupted {} streaming tasks", result);
    return result;
  }

  @Before(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamManager.killAllStreamsFor(..))")
  public void logKillAllStreamsFor(@NotNull JoinPoint jp) {
    final VideoStreamLocatorPlaylist playlist = (VideoStreamLocatorPlaylist) jp.getArgs()[0];
    logger.info("Killing all streams for VideoStreamLocatorPlaylist: {}", playlist.getId());
  }

  @Before(
      "execution(* net.tomasbot.matchday.api.service.video.VideoStreamManager.killStreamingTask(..))")
  public void logKillStreamingTask(@NotNull JoinPoint jp) {
    final VideoStreamLocator locator = (VideoStreamLocator) jp.getArgs()[0];
    logger.info("Killing streaming task for VideoStreamLocator: {}", locator.getStreamLocatorId());
  }
}
