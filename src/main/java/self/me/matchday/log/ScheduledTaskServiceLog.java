/*
 * Copyright (c) 2023.
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

import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.ScheduledTaskService;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;

@Aspect
public class ScheduledTaskServiceLog {

  private static final Logger logger = LogManager.getLogger(ScheduledTaskService.class);

  @Before("execution(* self.me.matchday.api.service.ScheduledTaskService.refreshEventData())")
  public void logRefreshAllDataSources() {
    logger.info("Refreshing all Data Sources with default SnapshotRequest...");
  }

  @AfterReturning(
      value =
          "execution(* self.me.matchday.api.service.ScheduledTaskService.getLatestEventDate(..))",
      returning = "latest")
  public void logGetLatestEventDate(@NotNull Object latest) {
    logger.info("Found latest Event date for refresh: {}", latest);
  }

  @Before("execution(* self.me.matchday.api.service.ScheduledTaskService.pruneVideoData())")
  public void logPruneVideoData() {
    logger.info("Pruning video data more than 2 weeks old...");
  }

  @Around("execution(* self.me.matchday.api.service.ScheduledTaskService.isVideoDataStale(..))")
  public Object logIsVideoDataStale(@NotNull ProceedingJoinPoint jp) throws Throwable {
    VideoStreamLocatorPlaylist playlist = (VideoStreamLocatorPlaylist) jp.getArgs()[0];
    Path storageLocation = playlist.getStorageLocation();
    Long id = playlist.getId();
    logger.info(
        "Determining if video data is stale for VideoStreamLocatorPlaylist: {} at:\n{}",
        id,
        storageLocation);
    Boolean stale = (Boolean) jp.proceed();
    if (stale) {
      logger.info("Data at: {} is stale; it will be PERMANENTLY DELETED!", storageLocation);
    }
    return stale;
  }

  @Before("execution(* self.me.matchday.api.service.ScheduledTaskService.rescheduleTasks(..))")
  public void logRescheduleTasks() {
    logger.info("Rescheduling tasks...");
  }

  @Before("execution(* self.me.matchday.api.service.ScheduledTaskService.scheduleTasks(..))")
  public void logScheduleTasks() {
    logger.info("Scheduling tasks...");
  }
}
