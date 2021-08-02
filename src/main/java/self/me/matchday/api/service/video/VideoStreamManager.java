/*
 * Copyright (c) 2021.
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

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import self.me.matchday.api.service.EventFileService;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;
import self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static self.me.matchday.model.video.StreamJobState.JobStatus;

@Service
class VideoStreamManager {

  private final VideoStreamLocatorPlaylistService playlistService;
  private final VideoStreamLocatorService locatorService;
  private final EventFileService eventFileService;
  private final FFmpegPlugin ffmpegPlugin;

  @Autowired
  public VideoStreamManager(
      final VideoStreamLocatorPlaylistService playlistService,
      final VideoStreamLocatorService locatorService,
      final EventFileService eventFileService,
      final FFmpegPlugin ffmpegPlugin) {

    this.playlistService = playlistService;
    this.locatorService = locatorService;
    this.eventFileService = eventFileService;
    this.ffmpegPlugin = ffmpegPlugin;
  }

  public VideoStreamLocatorPlaylist createVideoStreamFrom(
      @NotNull final EventFileSource fileSource) {
    return playlistService.createVideoStreamPlaylist(fileSource);
  }

  public Optional<VideoStreamLocatorPlaylist> getLocalStreamFor(@NotNull final String fileSrcId) {
    return playlistService.getVideoStreamPlaylistFor(fileSrcId);
  }

  public void deleteLocalStream(@NotNull final VideoStreamLocatorPlaylist playlist)
      throws IOException {

    Log.i("VideoStreamManager", "Deleting stream locator playlist: " + playlist.getId());
    final List<VideoStreamLocator> streamLocators = playlist.getStreamLocators();
    for (VideoStreamLocator streamLocator : streamLocators) {
      locatorService.deleteStreamLocatorWithData(streamLocator);
    }
    // delete stream root dir
    final boolean storageDeleted = playlist.getStorageLocation().toFile().delete();
    if (!storageDeleted) {
      final String message =
          "Could not delete storage directory for VideoStreamLocatorPlaylist: " + playlist.getId();
      throw new IOException(message);
    }
    playlistService.deleteVideoStreamPlaylist(playlist);
  }

  @Async("VideoStreamExecutor")
  @SneakyThrows
  public void beginStreaming(@NotNull final VideoStreamLocator streamLocator) {

    try {
      updateLocatorTaskState(streamLocator, JobStatus.STARTED, 0.01);
      final EventFile eventFile = streamLocator.getEventFile();
      final Path playlistPath = streamLocator.getPlaylistPath();

      updateLocatorTaskState(streamLocator, JobStatus.BUFFERING, 0.01);
      final EventFile refreshedEventFile = eventFileService.refreshEventFile(eventFile, false);
      final URI videoDataLink = refreshedEventFile.getInternalUrl().toURI();
      updateLocatorTaskState(streamLocator, JobStatus.BUFFERING, 0.1);

      // Start stream
      final Thread streamTask = ffmpegPlugin.streamUris(playlistPath, videoDataLink);
      streamTask.start();
      updateLocatorTaskState(streamLocator, JobStatus.STREAMING, 0.1);

      // Wait for stream task to finish
      streamTask.join();
      updateLocatorTaskState(streamLocator, JobStatus.COMPLETED, 1.0);

    } catch (Throwable e) {
      updateLocatorTaskState(streamLocator, JobStatus.ERROR, -1.0);
      throw e;
    }
  }

  public int killAllStreams() {
    final int taskCount = ffmpegPlugin.getStreamingTaskCount();
    ffmpegPlugin.interruptAllStreamTasks();
    return taskCount;
  }

  public void killAllStreamsFor(@NotNull final VideoStreamLocatorPlaylist playlist) {
    playlist.getStreamLocators().forEach(this::killStreamingTask);
  }

  public void killStreamingTask(@NotNull final VideoStreamLocator streamLocator) {
    final Double completionRatio = streamLocator.getState().getCompletionRatio();
    updateLocatorTaskState(streamLocator, JobStatus.STOPPED, completionRatio);
    ffmpegPlugin.interruptStreamingTask(streamLocator.getPlaylistPath());
  }

  /**
   * Given a video stream playlist, determine if it is ready for a client to begin streaming
   *
   * @param locatorPlaylist The collection of streams for a particular Event
   * @return true/false - clients can begin streaming
   */
  public boolean isStreamReady(@NotNull final VideoStreamLocatorPlaylist locatorPlaylist) {
    final JobStatus jobStatus = locatorPlaylist.getState().getStatus();
    return jobStatus == JobStatus.COMPLETED || jobStatus == JobStatus.STREAMING;
  }

  private void updateLocatorTaskState(
      @NotNull final VideoStreamLocator streamLocator,
      @NotNull final JobStatus status,
      final Double completionRatio) {
    streamLocator.updateState(status, completionRatio);
    locatorService.saveStreamLocator(streamLocator);
  }
}
