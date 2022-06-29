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

package self.me.matchday.api.service.video;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import self.me.matchday.api.service.FileServerService;
import self.me.matchday.model.video.TaskListState;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;
import self.me.matchday.plugin.fileserver.FileServerPlugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static self.me.matchday.model.video.StreamJobState.JobStatus;

@Service
public class StreamDelayAdviceService {

  private final FileServerService fileServerService;
  private final Map<FileServerPlugin, Long> delayIndex = new HashMap<>();

  @Value("${video-resources.default-ping-time}")
  private long defaultPingTime;

  @Value("${video-resources.ffmpeg-startup-time}")
  private long ffmpegStartUpTime;

  @Autowired
  public StreamDelayAdviceService(final FileServerService fileServerService) {
    this.fileServerService = fileServerService;
  }

  public long getDelayAdvice(@NotNull final VideoStreamLocatorPlaylist locatorPlaylist) {

    final JobStatus status = locatorPlaylist.getState().getStatus();
    if (status.equals(JobStatus.ERROR)) {
      throw new IllegalStateException(
          String.format(
              "VideoStreamLocatorPlaylist: %s is in ERROR status", locatorPlaylist.getId()));
    }
    if (!isStreamReady(locatorPlaylist)) {
      final int stepsToComplete = JobStatus.COMPLETED.compareTo(status);
      final int locatorCount = locatorPlaylist.getStreamLocators().size();
      final long pingTime = getPingTime(locatorPlaylist);
      return computeWaitMillis(locatorCount, stepsToComplete, pingTime, ffmpegStartUpTime);
    }
    return 0;
  }

  /**
   * Given a video stream playlist, determine if it is ready for a client to begin streaming
   *
   * @param locatorPlaylist The collection of streams for a particular Event
   * @return true/false - clients can begin streaming
   */
  public boolean isStreamReady(@NotNull final VideoStreamLocatorPlaylist locatorPlaylist) {

    final TaskListState state = locatorPlaylist.getState();
    final JobStatus jobStatus = state.getStatus();
    final Double completionRatio = state.getCompletionRatio();

    final boolean status = jobStatus == JobStatus.COMPLETED || jobStatus == JobStatus.STREAMING;
    final boolean completion = completionRatio > .01;
    return status && completion;
  }

  public void pingActiveFileServers() {
    // todo - implement scheduling, implement results persistence
    fileServerService.getEnabledPlugins().forEach(this::pingFileServer);
  }

  /**
   * Ping the specified file server and record the result
   *
   * @param fileServerPlugin A file server
   */
  public void pingFileServer(@NotNull final FileServerPlugin fileServerPlugin) {

    long timeout = 3_000; // default
    try {
      final URL pluginUrl = fileServerPlugin.getHostname();
      // ping...
      final Instant pingStart = Instant.now();
      final URLConnection connection = pluginUrl.openConnection();
      timeout = connection.getConnectTimeout();
      connection.connect();
      final Instant pingEnd = Instant.now();

      final long pingTime = Duration.between(pingStart, pingEnd).toMillis();
      delayIndex.put(fileServerPlugin, pingTime);
    } catch (IOException e) {
      // something went wrong; use default
      delayIndex.put(fileServerPlugin, timeout);
    }
  }

  /**
   * Get the last ping time for the file server servicing the URLs in the given locator playlist.
   *
   * @param locatorPlaylist The VideoStreamLocatorPlaylist for which advice is sought
   * @return The most recent ping time for the file server, or the default if not available
   */
  private long getPingTime(@NotNull final VideoStreamLocatorPlaylist locatorPlaylist) {

    return locatorPlaylist.getStreamLocators().stream()
        .map(locator -> locator.getVideoFile().getExternalUrl())
        .findFirst()
        .map(fileServerService::getEnabledPluginForUrl)
        .map(delayIndex::get)
        .orElse(defaultPingTime);
  }

  /**
   * Formula to compute the recommended retry delay
   *
   * @param numStreams The number of video streams in the locator playlist
   * @param stepsToComplete The number of streaming phases before COMPLETED is reached
   * @param pingTime The ping time for the file server
   * @param ffmpegOverhead The startup time for FFMPEG
   * @return The recommended retry delay
   */
  private long computeWaitMillis(
      final int numStreams,
      final int stepsToComplete,
      final long pingTime,
      final long ffmpegOverhead) {
    return stepsToComplete * pingTime + ffmpegOverhead;
  }
}
