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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.video.VideoStreamLocatorService;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoStreamLocator;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Aspect
public class VideoStreamLocatorServiceLog {

  private static final Logger logger = LogManager.getLogger(VideoStreamLocatorService.class);

  @SuppressWarnings("unchecked cast")
  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamLocatorService.getAllStreamLocators(..))")
  public Object logGetAllStreamLocators(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Getting all VideoStreamLocators from database...");
    List<VideoStreamLocator> locators = (List<VideoStreamLocator>) jp.proceed();
    logger.info("Found: {} VideoStreamLocators", locators.size());
    return locators;
  }

  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamLocatorService.getStreamLocator(..))")
  public Object logGetStreamLocator(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object arg = jp.getArgs()[0];
    logger.info("Getting VideoStreamLocator with ID: {} from database", arg);
    Object result = jp.proceed();
    logger.debug("Retrieved VideoStreamLocator for ID: {}: {}", arg, result);
    return result;
  }

  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamLocatorService.getStreamLocatorFor(..))")
  public Object logGetStreamLocatorFor(@NotNull ProceedingJoinPoint jp) throws Throwable {
    VideoFile videoFile = (VideoFile) jp.getArgs()[0];
    UUID fileId = videoFile.getFileId();
    logger.info("Getting VideoStreamLocator for VideoFile: {} from database", fileId);
    Object result = jp.proceed();
    logger.debug("Retrieved VideoStreamLocator for VideoFile: {}: {}", fileId, result);
    return result;
  }

  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamLocatorService.createStreamLocator(..))")
  public Object logCreateStreamLocator(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Path storageLocation = (Path) jp.getArgs()[0];
    VideoFile videoFile = (VideoFile) jp.getArgs()[1];
    logger.info("Creating VideoStreamLocator at: {} for VideoFile: {}", storageLocation, videoFile);
    Object result = jp.proceed();
    logger.info("Created VideoStreamLocator: {}", result);
    return result;
  }

  @Before(
      "execution(* self.me.matchday.api.service.video.VideoStreamLocatorService.updateStreamLocator(..))")
  public void logUpdateStreamLocator(@NotNull JoinPoint jp) {
    logger.info("Updating VideoStreamLocator: {}", jp.getArgs()[0]);
  }

  @Before(
      "execution(* self.me.matchday.api.service.video.VideoStreamLocatorService.deleteStreamLocator(..))")
  public void logDeleteStreamLocator(@NotNull JoinPoint jp) {
    logger.info("Deleting VideoStreamLocator: {}", jp.getArgs()[0]);
  }

  @Before(
      "execution(* self.me.matchday.api.service.video.VideoStreamLocatorService.deleteStreamLocatorWithData(..))")
  public void logDeleteStreamLocatorWithData(@NotNull JoinPoint jp) {
    logger.info("Deleting VideoStreamLocator: {} WITH DATA!", jp.getArgs()[0]);
  }

  @After(
      "execution(* self.me.matchday.api.service.video.VideoStreamLocatorService.deleteStreamLocatorWithData(..))")
  public void logSuccessDeleteStreamLocatorWithData(@NotNull JoinPoint jp) {
    logger.info("Successfully deleted VideoStreamLocator: {} WITH DATA!", jp.getArgs()[0]);
  }
}