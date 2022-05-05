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
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.video.StreamDelayAdviceService;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;
import self.me.matchday.plugin.fileserver.FileServerPlugin;

@Aspect
public class StreamDelayAdviceServiceLog {

  private static final Logger logger = LogManager.getLogger(StreamDelayAdviceService.class);

  @Around(
      "execution(* self.me.matchday.api.service.video.StreamDelayAdviceService.getDelayAdvice(..))")
  public Object logGetDelayAdvice(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final VideoStreamLocatorPlaylist playlist = (VideoStreamLocatorPlaylist) jp.getArgs()[0];
    Long playlistId = playlist.getId();
    logger.info("Getting delay advice for VideoStreamLocatorPlaylist: {}", playlistId);
    Object result = jp.proceed();
    logger.info(
        "Got delay advice of: {} milliseconds for VideoStreamLocatorPlaylist: {}",
        result,
        playlistId);
    return result;
  }

  @Around(
      "execution(* self.me.matchday.api.service.video.StreamDelayAdviceService.isStreamReady(..))")
  public Object logIsStreamReady(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final VideoStreamLocatorPlaylist playlist = (VideoStreamLocatorPlaylist) jp.getArgs()[0];
    Object result = jp.proceed();
    logger.info(
        "Stream for VideoStreamLocatorPlaylist: {} is ready to begin streaming? {}",
        playlist.getId(),
        result);
    return result;
  }

  @Before(
      "execution(* self.me.matchday.api.service.video.StreamDelayAdviceService.pingActiveFileServers())")
  public void logPingActiveFileServers() {
    logger.info("Pinging all active file servers for stream delay advice...");
  }

  @Before(
      "execution(* self.me.matchday.api.service.video.StreamDelayAdviceService.pingFileServer(..))")
  public void logPingFileServer(@NotNull JoinPoint jp) {
    final FileServerPlugin plugin = (FileServerPlugin) jp.getArgs()[0];
    logger.info("Pinging file server for plugin: {}", plugin.getPluginId());
  }
}
