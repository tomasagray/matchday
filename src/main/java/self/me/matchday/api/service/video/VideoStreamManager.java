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

package self.me.matchday.api.service.video;

import static self.me.matchday.model.video.StreamJobState.JobStatus;

import java.io.*;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import self.me.matchday.config.VideoStreamingConfig;
import self.me.matchday.model.video.*;
import self.me.matchday.plugin.io.ffmpeg.FFmpegLogger;
import self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import self.me.matchday.plugin.io.ffmpeg.FFmpegStreamTask;
import self.me.matchday.util.RecursiveDirectoryDeleter;

@Service
public class VideoStreamManager {

  private final VideoStreamLocatorPlaylistService playlistService;
  private final VideoStreamLocatorService locatorService;
  private final VideoFileService videoFileService;
  private final FFmpegPlugin ffmpegPlugin;
  private final Map<Long, Future<Long>> streamQueue = new HashMap<>();
  private final ThreadPoolTaskExecutor executor;

  @Value("${video-resources.file-read-buffer-size}")
  private int BUFFER_SIZE;

  @Value("${video-resources.file-recheck-delay-ms}")
  private int FILE_CHECK_DELAY;

  @Value("${video-resources.max-recheck-seconds}")
  private int MAX_RECHECK_TIMEOUT;

  public VideoStreamManager(
      final VideoStreamLocatorPlaylistService playlistService,
      final VideoStreamLocatorService locatorService,
      final VideoFileService videoFileService,
      final VideoStreamingConfig streamingConfig,
      final FFmpegPlugin ffmpegPlugin) {

    this.playlistService = playlistService;
    this.locatorService = locatorService;
    this.videoFileService = videoFileService;
    this.ffmpegPlugin = ffmpegPlugin;

    this.executor = streamingConfig.getAsyncExecutor();
  }

  public VideoStreamLocatorPlaylist createVideoStreamFrom(
      @NotNull final VideoFileSource fileSource) {
    return playlistService.createVideoStreamPlaylist(fileSource);
  }

  public void queueStreamJobs(@NotNull VideoStreamLocatorPlaylist playlist) {
    playlist.getStreamLocators().stream()
        // ensure streams are started in correct order
        .sorted(Comparator.comparing(VideoStreamLocator::getVideoFile))
        .forEach(this::queueStreamJob);
  }

  public void queueStreamJob(@NotNull VideoStreamLocator locator) {
    updateLocatorTaskState(locator, JobStatus.QUEUED, 0d);
    final Future<Long> streamTask =
        executor.submit(
            () -> {
              beginStreaming(locator);
              return locator.getStreamLocatorId();
            });
    streamQueue.put(locator.getStreamLocatorId(), streamTask);
  }

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
        // ^ JobStatus updated to COMPLETED in above method
      } else {
        streamWithoutLog(streamTask);
        updateLocatorTaskState(streamLocator, JobStatus.COMPLETED, 1.0);
      }
    } catch (Throwable e) {
      setLocatorErrorState(streamLocator, e);
      throw new VideoStreamingException(e);
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
        .doOnComplete(
            () -> {
              final JobStatus previousStatus =
                  locatorService
                      .getStreamLocator(streamLocator.getStreamLocatorId())
                      .map(VideoStreamLocator::getState)
                      .map(TaskState::getStatus)
                      .orElse(JobStatus.COMPLETED);
              if (previousStatus.compareTo(JobStatus.STOPPED) > 0) {
                updateLocatorTaskState(streamLocator, JobStatus.COMPLETED, 1.0);
              }
            })
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

  public Optional<VideoStreamLocatorPlaylist> getLocalStreamFor(@NotNull final UUID fileSrcId) {
    return playlistService.getVideoStreamPlaylistFor(fileSrcId);
  }

  /**
   * Read playlist file from disk and return as a String
   *
   * @param partId Playlist locator ID
   * @return The playlist as a String
   */
  public String readPlaylistFile(@NotNull final Long partId) throws Exception {
    Optional<VideoStreamLocator> locatorOptional = locatorService.getStreamLocator(partId);
    if (locatorOptional.isPresent()) {
      VideoStreamLocator locator = locatorOptional.get();
      return readLocatorPlaylist(locator);
    } else {
      throw new IllegalArgumentException("No VideoStreamLocator found for ID: " + partId);
    }
  }

  /**
   * Read playlist file; it may be concurrently being written to, so we read it reactively
   *
   * @param streamLocator The locator pointing to the required playlist file
   * @return The playlist file as a String or empty
   */
  private @Nullable String readLocatorPlaylist(@NotNull final VideoStreamLocator streamLocator)
      throws Exception {

    final StringBuilder sb = new StringBuilder();
    final Path playlistPath = streamLocator.getPlaylistPath();
    // wait until playlist file actually exists
    waitForFile(playlistPath);

    final Flux<DataBuffer> fluxBuffer =
        DataBufferUtils.read(playlistPath, new DefaultDataBufferFactory(), BUFFER_SIZE);
    fluxBuffer
        .publishOn(Schedulers.boundedElastic())
        .buffer(450)
        .flatMapIterable(Function.identity())
        .doOnNext(buffer -> readBufferFromDisk(buffer, sb))
        .blockLast();
    final String result = sb.toString();
    return result.isEmpty() ? null : result;
  }

  private void waitForFile(@NotNull Path playlistPath) throws IOException, InterruptedException {

    final File playlistFile = playlistPath.toFile();
    final Duration timeout = Duration.of(MAX_RECHECK_TIMEOUT, ChronoUnit.SECONDS);
    final Instant start = Instant.now();

    while (!playlistFile.exists()) {
      TimeUnit.MILLISECONDS.sleep(FILE_CHECK_DELAY);
      final Duration elapsed = Duration.between(start, Instant.now());
      if (elapsed.compareTo(timeout) > 0) {
        throw new IOException("Timeout exceeded reading playlist file: " + playlistFile);
      }
    }
  }

  private void readBufferFromDisk(@NotNull DataBuffer buffer, @NotNull StringBuilder sb) {
    try (final InputStream inputStream = buffer.asInputStream()) {
      final String data = new String(inputStream.readAllBytes());
      sb.append(data);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public int getActiveStreamCount() {
    return ffmpegPlugin.getStreamingTaskCount();
  }

  public int killAllStreams() {
    final int taskCount = ffmpegPlugin.getStreamingTaskCount();
    ffmpegPlugin.interruptAllStreamTasks();
    return taskCount;
  }

  public int killAllStreamsFor(@NotNull final VideoStreamLocatorPlaylist playlist) {

    int killCount = 0;
    final List<VideoStreamLocator> locators = playlist.getStreamLocators();
    for (final VideoStreamLocator locator : locators) {
      killStreamingTask(locator);
      killCount++;
    }
    return killCount;
  }

  public void killStreamingTask(@NotNull final VideoStreamLocator streamLocator) {
    final Double completionRatio = streamLocator.getState().getCompletionRatio();
    // cancel process
    ffmpegPlugin.interruptStreamingTask(streamLocator.getPlaylistPath());
    // cancel task
    final Future<Long> task = streamQueue.get(streamLocator.getStreamLocatorId());
    if (task != null) {
      task.cancel(true);
    }
    updateLocatorTaskState(streamLocator, JobStatus.STOPPED, completionRatio);
  }

  public void deleteLocalStreams(@NotNull final VideoStreamLocatorPlaylist playlist)
      throws IOException {

    final List<VideoStreamLocator> streamLocators = playlist.getStreamLocators();
    playlistService.deleteVideoStreamPlaylist(playlist);
    for (VideoStreamLocator streamLocator : streamLocators) {
      locatorService.deleteStreamLocator(streamLocator);
      deleteVideoDataFromDisk(streamLocator);
    }
    // delete stream root dir
    final File storageLocation = playlist.getStorageLocation().toFile();
    if (storageLocation.exists()) {
      final boolean storageDeleted = storageLocation.delete();
      if (!storageDeleted) {
        final String message =
            "Could not delete storage directory for VideoStreamLocatorPlaylist: " + playlist;
        throw new IOException(message);
      }
    }
  }

  public void deleteStreamLocatorWithData(@NotNull final VideoStreamLocator streamLocator)
      throws IOException {

    // remove locator from playlist
    final Long locatorId = streamLocator.getStreamLocatorId();
    final Optional<VideoStreamLocatorPlaylist> playlistOptional =
        playlistService.getVideoStreamPlaylistContaining(locatorId);
    if (playlistOptional.isPresent()) {
      final VideoStreamLocatorPlaylist playlist = playlistOptional.get();
      // update playlist
      final boolean removed = playlist.getStreamLocators().remove(streamLocator);
      if (!removed) {
        throw new IllegalArgumentException(
            "VideoStreamLocatorPlaylist does not contain requested Locator ID. "
                + "Check database for corruption!");
      }
      playlist.getState().removeTaskState(streamLocator.getState());
      locatorService.deleteStreamLocator(streamLocator);
      deleteVideoDataFromDisk(streamLocator);
    } else {
      throw new IllegalArgumentException(
          "Cannot delete non-existent VideoStreamLocator: " + locatorId);
    }
  }

  private void deleteVideoDataFromDisk(@NotNull VideoStreamLocator streamLocator)
      throws IOException {
    final Path playlistPath = streamLocator.getPlaylistPath();
    final File playlistFile = playlistPath.toFile();
    // if the file doesn't exist, just return
    if (!playlistFile.exists()) {
      return;
    }
    if (!playlistFile.isFile()) {
      final String msg =
          String.format(
              "VideoStreamLocator: %s does not refer to a file! Will not delete",
              streamLocator.getStreamLocatorId());
      throw new IllegalArgumentException(msg);
    }

    // delete the data
    final Path streamDataDir = playlistPath.getParent();
    Files.walkFileTree(streamDataDir, new RecursiveDirectoryDeleter());
    if (streamDataDir.toFile().exists()) {
      throw new IOException("Could not delete data at: " + streamDataDir);
    }
  }

  private void updateLocatorTaskState(
      @NotNull final VideoStreamLocator streamLocator,
      @NotNull final JobStatus status,
      final Double completionRatio) {
    streamLocator.updateState(status, completionRatio);
    locatorService.updateStreamLocator(streamLocator);
  }

  private void setLocatorErrorState(
      @NotNull VideoStreamLocator streamLocator, @NotNull Throwable error) {
    streamLocator.updateState(JobStatus.ERROR, -1.0, new VideoStreamingError(error));
    locatorService.updateStreamLocator(streamLocator);
  }
}
