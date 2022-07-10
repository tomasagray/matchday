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

package self.me.matchday.log.plugin.ffmpeg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import self.me.matchday.plugin.io.ffmpeg.FFmpegStreamTask;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

@Aspect
public class FFmpegLog {

  private static final Logger logger = LogManager.getLogger(FFmpegPlugin.class);

  @Around(
      "execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegConcatStreamTask.createConcatFile())")
  public Object logCreateConcatFile(@NotNull ProceedingJoinPoint jp) throws Throwable {
    try {
      Object logFile = jp.proceed();
      logger.info("Created concat text file: {}", logFile);
      return logFile;
    } catch (Throwable e) {
      logger.error("There was a problem creating the concat file for FFMPEG: {}", e.getMessage());
      throw e;
    }
  }

  @Before("execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin.interruptAllStreamTasks())")
  public void logInterruptAllStreamingTasks() {
    logger.info("Killing all running stream tasks...");
  }

  @Before("execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin.interruptStreamingTask(..))")
  public void logInterruptStreamingTask(@NotNull JoinPoint jp) {
    logger.info("Killing streaming task to: {}", jp.getArgs()[0]);
  }

  @Around("execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin.getStreamingTaskCount())")
  public Object logGetStreamingTaskCount(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object taskCount = jp.proceed();
    logger.info("There are currently: {} active streaming tasks.", taskCount);
    return taskCount;
  }

  @Before("execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin.readFileMetadata(..))")
  public void logReadFileMetadata(@NotNull JoinPoint jp) {
    logger.info("Getting media metadata using FFprobe from: {}", jp.getArgs()[0]);
  }

  @Around("execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegStreamTask.run())")
  public Object logRunFfmpegStreamTask(@NotNull ProceedingJoinPoint jp) throws Throwable {
    FFmpegStreamTask task = (FFmpegStreamTask) jp.getTarget();
    Path playlistPath = task.getPlaylistPath();
    logger.info("Running FFMPEG stream task to: {} ...", playlistPath);
    Instant start = Instant.now();
    Object result = jp.proceed();
    Instant end = Instant.now();
    long duration = Duration.between(start, end).getSeconds();
    logger.debug(
        "FFMPEG stream execution to: {} took: {}",
        playlistPath,
        String.format("%d:%02d:%02d", duration / 3600, (duration % 3600) / 60, (duration % 60)));
    return result;
  }

  @Around("execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegStreamTask.kill())")
  public Object logKillFfmpegTask(@NotNull ProceedingJoinPoint jp) throws Throwable {
    FFmpegStreamTask task = (FFmpegStreamTask) jp.getTarget();
    logger.info("Killing FFMPEG stream task to: {} ...", task.getPlaylistPath());
    Boolean killed = (Boolean) jp.proceed();
    logger.info("FFMPEG stream task successfully killed? {}", killed);
    return killed;
  }

  @Around("execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegStreamTask.getExecCommand())")
  public Object logGetFFmpegStreamTaskCommand(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object command = jp.proceed();
    logger.info("Executing FFMPEG command:\n{}", command);
    return command;
  }

  @Before("execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegStreamTask.prepareStream())")
  public void logPrepareFfmpegStream() {
    logger.debug("Preparing FFMPEG stream for execution...");
  }

  @Around("execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegStreamTask.getInputString())")
  public Object logGetFfmpegInputString(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object input = jp.proceed();
    logger.debug("Sending input to FFMPEG: {}", input);
    return input;
  }
}
