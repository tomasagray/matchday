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

import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.video.VideoFileSelectorService;
import self.me.matchday.model.Event;
import self.me.matchday.model.video.VideoFileSource;

@Aspect
public class VideoFileSelectorServiceLog {

  private static final Logger logger = LogManager.getLogger(VideoFileSelectorService.class);

  @Around(
      "execution(* self.me.matchday.api.service.video.VideoFileSelectorService.getBestFileSource(..))")
  public Object logGetBestFileSource(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final Event event = (Event) jp.getArgs()[0];
    UUID eventId = event.getEventId();
    logger.info("Determining \"best\" VideoFileSource for Event: {}", eventId);
    Object result = jp.proceed();
    logger.info("Determined \"best\" VideoFileSource for Event: {} is {}", eventId, result);
    return result;
  }

  @Around(
      "execution(* self.me.matchday.api.service.video.VideoFileSelectorService.getPlaylistFiles(..))")
  public Object logGetPlaylistFiles(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final VideoFileSource fileSource = (VideoFileSource) jp.getArgs()[0];
    UUID fileSrcId = fileSource.getFileSrcId();
    logger.info("Getting best version of each VideoFile for VideoFileSource: {}", fileSrcId);
    Object result = jp.proceed();
    logger.info("Best VideoFilePack for VideoFileSource: {}: {}", fileSrcId, result);
    return result;
  }
}
