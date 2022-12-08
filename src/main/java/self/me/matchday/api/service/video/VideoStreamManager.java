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

import static self.me.matchday.model.video.StreamJobState.JobStatus;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import self.me.matchday.model.video.TaskState;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;
import self.me.matchday.plugin.io.ffmpeg.FFmpegLogger;
import self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import self.me.matchday.plugin.io.ffmpeg.FFmpegStreamTask;

@Service
@Transactional
public class VideoStreamManager {

  @Value("${video-resources.file-read-buffer-size}")
  private int BUFFER_SIZE;

  @Value("${video-resources.file-recheck-delay-ms}")
  private int FILE_CHECK_DELAY;

  @Value("${video-resources.max-recheck-seconds}")
  private int MAX_RECHECK_TIMEOUT;

  private final VideoStreamLocatorPlaylistService playlistService;
  private final VideoStreamLocatorService locatorService;
  private final VideoFileService videoFileService;
  private final FFmpegPlugin ffmpegPlugin;

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

  public void queueStreamJobs(@NotNull List<VideoStreamLocator> locators) {
    locators.stream()
        // ensure streams are started in correct order
        .sorted(Comparator.comparing(VideoStreamLocator::getVideoFile))
        .peek(locator -> updateLocatorTaskState(locator, JobStatus.QUEUED, 0d))
        .forEach(this::beginStreaming);
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
      updateLocatorTaskState(streamLocator, JobStatus.ERROR, -1.0);
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

  public Optional<VideoStreamLocatorPlaylist> getLocalStreamFor(@NotNull final UUID fileSrcId) {
    return playlistService.getVideoStreamPlaylistFor(fileSrcId);
  }

  /**
   * Read playlist file from disk and return as a String
   *
   * @param partId Playlist locator ID
   * @return The playlist as a String
   */
  public Optional<String> readPlaylistFile(@NotNull final Long partId) {
    return locatorService.getStreamLocator(partId).map(this::readLocatorPlaylist);
  }

  /**
   * Read playlist file; it may be concurrently being written to, so we read it reactively
   *
   * @param streamLocator The locator pointing to the required playlist file
   * @return The playlist file as a String or empty
   */
  private @Nullable String readLocatorPlaylist(@NotNull final VideoStreamLocator streamLocator) {

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

  private void waitForFile(@NotNull Path playlistPath) {

    final File playlistFile = playlistPath.toFile();
    final Duration timeout = Duration.of(MAX_RECHECK_TIMEOUT, ChronoUnit.SECONDS);
    final Instant start = Instant.now();
    try {
      while (!playlistFile.exists()) {
        TimeUnit.MILLISECONDS.sleep(FILE_CHECK_DELAY);
        final Duration elapsed = Duration.between(start, Instant.now());
        if (elapsed.compareTo(timeout) > 0) {
          throw new InterruptedException("Timeout exceeded reading playlist file: " + playlistFile);
        }
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
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
    ffmpegPlugin.interruptStreamingTask(streamLocator.getPlaylistPath());
    updateLocatorTaskState(streamLocator, JobStatus.STOPPED, completionRatio);
  }

  public void deleteLocalStreams(@NotNull final VideoStreamLocatorPlaylist playlist)
      throws IOException {

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

  private void updateLocatorTaskState(
      @NotNull final VideoStreamLocator streamLocator,
      @NotNull final JobStatus status,
      final Double completionRatio) {
    streamLocator.updateState(status, completionRatio);
    locatorService.updateStreamLocator(streamLocator);
  }
}
