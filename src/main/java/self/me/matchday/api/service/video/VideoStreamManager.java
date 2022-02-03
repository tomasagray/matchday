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
import reactor.core.publisher.Flux;
import self.me.matchday.api.service.VideoFileService;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;
import self.me.matchday.plugin.io.ffmpeg.FFmpegLogger;
import self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import self.me.matchday.plugin.io.ffmpeg.FFmpegStreamTask;
import self.me.matchday.util.Log;

import java.io.*;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static self.me.matchday.model.video.StreamJobState.JobStatus;

@Service
class VideoStreamManager {

  private final VideoStreamLocatorPlaylistService playlistService;
  private final VideoStreamLocatorService locatorService;
  private final VideoFileService videoFileService;
  private final FFmpegPlugin ffmpegPlugin;

  @Autowired
  public VideoStreamManager(
      final VideoStreamLocatorPlaylistService playlistService,
      final VideoStreamLocatorService locatorService,
      final VideoFileService videoFileService,
      final FFmpegPlugin ffmpegPlugin) {

    this.playlistService = playlistService;
    this.locatorService = locatorService;
    this.videoFileService = videoFileService;
    this.ffmpegPlugin = ffmpegPlugin;
  }

  public VideoStreamLocatorPlaylist createVideoStreamFrom(
      @NotNull final VideoFileSource fileSource) {
    return playlistService.createVideoStreamPlaylist(fileSource);
  }

  public Optional<VideoStreamLocatorPlaylist> getLocalStreamFor(@NotNull final UUID fileSrcId) {
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
    final File storageLocation = playlist.getStorageLocation().toFile();
    if (storageLocation.exists()) {
      final boolean storageDeleted = storageLocation.delete();
      if (!storageDeleted) {
        final String message =
            "Could not delete storage directory for VideoStreamLocatorPlaylist: "
                + playlist.getId();
        throw new IOException(message);
      }
    }
    playlistService.deleteVideoStreamPlaylist(playlist);
  }

  // todo - extract to strategy pattern
  @SneakyThrows
  @Async("VideoStreamExecutor")
  public void beginStreaming(@NotNull final VideoStreamLocator streamLocator) {
    try {
      final VideoFile videoFile = streamLocator.getVideoFile();
      final Path playlistPath = streamLocator.getPlaylistPath();

      updateLocatorTaskState(streamLocator, JobStatus.STARTED, 0.0);
      final VideoFile refreshedVideoFile = videoFileService.refreshVideoFile(videoFile, false);
      final URI videoDataLink = refreshedVideoFile.getInternalUrl().toURI();
      updateLocatorTaskState(streamLocator, JobStatus.BUFFERING, 0.0);

      // Start stream
      final FFmpegStreamTask streamTask = ffmpegPlugin.streamUris(playlistPath, videoDataLink);
      updateLocatorTaskState(streamLocator, JobStatus.STREAMING, 0.0);
      if (streamTask.isLoggingEnabled()) {
        streamWithLogging(streamLocator, streamTask);
      } else {
        streamWithoutLog(streamTask);
        updateLocatorTaskState(streamLocator, JobStatus.COMPLETED, 1.0);
      }
    } catch (Throwable e) {
      updateLocatorTaskState(streamLocator, JobStatus.ERROR, -1.0);
      throw e;
    }
  }

  private void streamWithLogging(
      @NotNull final VideoStreamLocator streamLocator, @NotNull final FFmpegStreamTask streamTask)
      throws IOException {

    final Process streamProcess = streamTask.execute();
    final FFmpegLogger logger = new FFmpegLogger(streamProcess, streamTask.getDataDir());
    final FFmpegLogAdapter logReader = new FFmpegLogAdapter(locatorService, streamLocator);
    final OpenOption[] options = {StandardOpenOption.CREATE, StandardOpenOption.WRITE};
    AsynchronousFileChannel fw = AsynchronousFileChannel.open(logger.getLogFile(), options);
    final Flux<String> logEmitter = logger.beginLogging(fw);
    logEmitter
        .doOnNext(logReader)
        .doOnComplete(() -> updateLocatorTaskState(streamLocator, JobStatus.COMPLETED, 1.0))
        .subscribe();
  }

  private void streamWithoutLog(@NotNull final FFmpegStreamTask streamTask)
      throws IOException, InterruptedException {

    final Process process = streamTask.execute();
    // absorb process output
    final InputStream errorStream = process.getErrorStream();
    final BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
    reader.lines().forEach(line -> {});
    // wait for process to finish
    process.waitFor();
    process.destroy();
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

  private void updateLocatorTaskState(
      @NotNull final VideoStreamLocator streamLocator,
      @NotNull final JobStatus status,
      final Double completionRatio) {
    streamLocator.updateState(status, completionRatio);
    locatorService.saveStreamLocator(streamLocator);
  }
}
