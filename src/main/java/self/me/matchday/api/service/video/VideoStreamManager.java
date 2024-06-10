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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import self.me.matchday.model.video.TaskState;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;
import self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import self.me.matchday.util.RecursiveDirectoryDeleter;

@Service
public class VideoStreamManager {

  private final VideoStreamLocatorPlaylistService playlistService;
  private final VideoStreamLocatorService locatorService;
  private final FFmpegPlugin ffmpegPlugin;
  private final VideoStreamer videoStreamer;
  private final Map<Long, Future<Long>> streamQueue = new HashMap<>();

  @Value("${video-resources.file-read-buffer-size}")
  private int BUFFER_SIZE;

  @Value("${video-resources.file-recheck-delay-ms}")
  private int FILE_CHECK_DELAY;

  @Value("${video-resources.max-recheck-seconds}")
  private int MAX_RECHECK_TIMEOUT;

  public VideoStreamManager(
      VideoStreamLocatorPlaylistService playlistService,
      VideoStreamLocatorService locatorService,
      FFmpegPlugin ffmpegPlugin,
      VideoStreamer videoStreamer) {
    this.playlistService = playlistService;
    this.locatorService = locatorService;
    this.ffmpegPlugin = ffmpegPlugin;
    this.videoStreamer = videoStreamer;
  }

  private static void deleteStorageLocation(@NotNull VideoStreamLocatorPlaylist playlist)
      throws IOException {
    Path storageLocation = playlist.getStorageLocation();
    File storageFile = storageLocation.toFile();
    if (storageFile.exists()) {
      Files.walkFileTree(storageLocation, new RecursiveDirectoryDeleter());
      // ensure delete was successful
      if (storageFile.exists()) {
        final String message =
            "Could not delete storage directory for VideoStreamLocatorPlaylist: " + playlist;
        throw new IOException(message);
      }
    }
  }

  public VideoStreamLocatorPlaylist createVideoStreamFrom(@NotNull VideoFileSource fileSource) {
    UUID fileSrcId = fileSource.getFileSrcId();
    Optional<VideoStreamLocatorPlaylist> existing =
        playlistService.getVideoStreamPlaylistFor(fileSrcId);
    if (existing.isPresent()) {
      throw new VideoStreamingException(
          "Video stream has already started for VideoFileSource: " + fileSrcId);
    }
    return playlistService.createVideoStreamPlaylist(fileSource);
  }

  public void queueStreamJobs(@NotNull VideoStreamLocatorPlaylist playlist) {
    List<VideoStreamLocator> sortedLocators =
        playlist.getStreamLocators().stream()
            // ensure streams are started in correct order
            .sorted(Comparator.comparing(VideoStreamLocator::getVideoFile))
            .toList();
    for (VideoStreamLocator locator : sortedLocators) {
      queueStreamJob(locator);
    }
  }

  public void queueStreamJob(@NotNull VideoStreamLocator locator) {
    videoStreamer.updateLocatorTaskState(locator, new TaskState(JobStatus.QUEUED, 0.0));
    final Long locatorId = locator.getStreamLocatorId();
    final Future<Long> streamTask =
        videoStreamer.beginStreaming(
            locator,
            new Runnable() {
              private boolean hasRun = false;

              @Override
              public void run() {
                if (!hasRun) {
                  streamQueue.remove(locatorId);
                  hasRun = true;
                }
              }
            });
    streamQueue.put(locatorId, streamTask);
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

  public int killAllStreamsFor(@NotNull VideoStreamLocatorPlaylist playlist) {
    int killCount = 0;
    final List<VideoStreamLocator> locators = playlist.getStreamLocators();
    for (final VideoStreamLocator locator : locators) {
      killStreamingTask(locator);
      killCount++;
    }
    return killCount;
  }

  public void killStreamingTask(@NotNull VideoStreamLocator streamLocator) {
    Long locatorId = streamLocator.getStreamLocatorId();
    Double completionRatio = streamLocator.getState().getCompletionRatio();
    // cancel process
    ffmpegPlugin.interruptStreamingTask(streamLocator.getPlaylistPath());
    // cancel task
    Future<Long> task = streamQueue.get(locatorId);
    if (task != null) {
      task.cancel(true);
      streamQueue.remove(locatorId);
    }
    videoStreamer.updateLocatorTaskState(
        streamLocator, new TaskState(JobStatus.STOPPED, completionRatio));
  }

  public void deleteLocalStreams(@NotNull final VideoStreamLocatorPlaylist playlist)
      throws IOException {
    final List<VideoStreamLocator> streamLocators = playlist.getStreamLocators();
    playlistService.deleteVideoStreamPlaylist(playlist);
    for (VideoStreamLocator streamLocator : streamLocators) {
      deleteVideoDataFromDisk(streamLocator);
    }
    // delete stream root dir
    deleteStorageLocation(playlist);
  }

  @Transactional
  public void deleteStreamLocatorWithData(@NotNull final VideoStreamLocator streamLocator)
      throws IOException {
    // remove locator from playlist
    final Long locatorId = streamLocator.getStreamLocatorId();
    final Optional<VideoStreamLocatorPlaylist> playlistOptional =
        playlistService.getVideoStreamPlaylistContaining(locatorId);
    if (playlistOptional.isPresent()) {
      final VideoStreamLocatorPlaylist playlist = playlistOptional.get();
      // update playlist
      playlist.getState().removeTaskState(streamLocator.getState());
      List<VideoStreamLocator> streamLocators = playlist.getStreamLocators();
      streamLocators.remove(streamLocator);
      if (streamLocators.isEmpty()) {
        playlistService.deleteVideoStreamPlaylist(playlist);
      }
      // delete locator & data
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
}
