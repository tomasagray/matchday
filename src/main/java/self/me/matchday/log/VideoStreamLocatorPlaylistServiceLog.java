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

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.video.VideoStreamLocatorPlaylistService;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;

@Aspect
public class VideoStreamLocatorPlaylistServiceLog {

  private static final Logger logger =
      LogManager.getLogger(VideoStreamLocatorPlaylistService.class);

  @SuppressWarnings("unchecked cast")
  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamLocatorPlaylistService.getAllVideoStreamPlaylists())")
  public Object logGetAllVideoStreamPlaylists(@NotNull ProceedingJoinPoint jp) throws Throwable {

    logger.info("Retrieving all VideoStreamLocatorPlaylists...");
    final List<VideoStreamLocatorPlaylist> playlists =
        (List<VideoStreamLocatorPlaylist>) jp.proceed();
    logger.info("Found {} playlists", playlists.size());
    return playlists;
  }

  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamLocatorPlaylistService.createVideoStreamPlaylist(..))")
  public Object logCreateVideoStreamPlaylist(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final VideoFileSource fileSource = (VideoFileSource) jp.getArgs()[0];
    logger.info("Creating VideoStreamPlaylist for VideoFileSource: {}", fileSource.getFileSrcId());
    final Object result = jp.proceed();
    logger.debug("Created VideoStreamPlaylist: {}", result);
    return result;
  }

  @Around(
      "execution(* self.me.matchday.api.service.video.VideoStreamLocatorPlaylistService.getVideoStreamPlaylistFor(..))")
  public Object logGetVideoStreamPlaylistFor(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Getting VideoStreamLocatorPlaylist for VideoFileSource: {}", jp.getArgs()[0]);
    final Object result = jp.proceed();
    logger.info("Found VideoStreamLocatorPlaylist: {}", result);
    return result;
  }

  @Before(
      "execution(* self.me.matchday.api.service.video.VideoStreamLocatorPlaylistService.deleteVideoStreamPlaylist(..))")
  public void logDeleteVideoStreamPlaylist(@NotNull JoinPoint jp) {
    final VideoStreamLocatorPlaylist playlist = (VideoStreamLocatorPlaylist) jp.getArgs()[0];
    logger.info("Deleting VideoStreamPlaylist for: {}", playlist.getId());
  }
}
