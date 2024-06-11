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

package net.tomasbot.matchday.log.plugin.ffmpeg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.plugin.io.ffmpeg.FFmpegPlugin;

@Aspect
public class FFmpegPluginLog {

  private static final Logger logger = LogManager.getLogger(FFmpegPlugin.class);

  @Before(
      "execution(* net.tomasbot.matchday.plugin.io.ffmpeg.FFmpegPlugin.interruptAllStreamTasks())")
  public void logInterruptAllStreamingTasks() {
    logger.info("Killing all running stream tasks...");
  }

  @Before(
      "execution(* net.tomasbot.matchday.plugin.io.ffmpeg.FFmpegPlugin.interruptStreamingTask(..))")
  public void logInterruptStreamingTask(@NotNull JoinPoint jp) {
    logger.info("Killing streaming task to: {}", jp.getArgs()[0]);
  }

  @Around(
      "execution(* net.tomasbot.matchday.plugin.io.ffmpeg.FFmpegPlugin.getStreamingTaskCount())")
  public Object logGetStreamingTaskCount(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object taskCount = jp.proceed();
    logger.info("There are currently: {} active streaming tasks.", taskCount);
    return taskCount;
  }

  @Before("execution(* net.tomasbot.matchday.plugin.io.ffmpeg.FFmpegPlugin.readFileMetadata(..))")
  public void logReadFileMetadata(@NotNull JoinPoint jp) {
    logger.info("Getting media metadata using FFprobe from: {}", jp.getArgs()[0]);
  }
}
