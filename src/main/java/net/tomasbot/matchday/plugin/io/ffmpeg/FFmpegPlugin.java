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

package net.tomasbot.matchday.plugin.io.ffmpeg;

import static net.tomasbot.matchday.config.settings.plugin.FFmpegBaseArgs.FFMPEG_BASE_ARGS;
import static net.tomasbot.matchday.config.settings.plugin.FFprobeBaseArgs.FFPROBE_BASE_ARGS;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import net.tomasbot.ffmpeg_wrapper.FFmpeg;
import net.tomasbot.ffmpeg_wrapper.FFprobe;
import net.tomasbot.ffmpeg_wrapper.metadata.FFmpegMetadata;
import net.tomasbot.ffmpeg_wrapper.request.TranscodeRequest;
import net.tomasbot.ffmpeg_wrapper.task.FFmpegStreamTask;
import net.tomasbot.matchday.api.service.SettingsService;
import net.tomasbot.matchday.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class FFmpegPlugin implements Plugin {
  
  private final FFmpegPluginProperties pluginProperties;
  private final SettingsService settingsService;

  private final Map<Path, FFmpegStreamTask> streamingTasks = new ConcurrentSkipListMap<>();
  private final String ffmpegExec;
  private final String ffprobeExec;

  public FFmpegPlugin(FFmpegPluginProperties pluginProperties, SettingsService settingsService) {
    this.pluginProperties = pluginProperties;
    this.settingsService = settingsService;
    this.ffmpegExec = pluginProperties.getFFmpegLocation();
    this.ffprobeExec = pluginProperties.getFFprobeLocation();
  }

  /**
   * Create an HLS stream from a given collection of URIs
   *
   * @param transcodeRequest Encapsulated parameters for streaming
   * @return The path of the playlist file produced by FFMPEG
   */
  @SuppressWarnings("unchecked cast")
  public FFmpegStreamTask streamUri(@NotNull TranscodeRequest transcodeRequest) {
    // Get absolute path for task key
    final Path absolutePath = transcodeRequest.getTo().toAbsolutePath();
    checkTaskAlreadyExecuting(absolutePath);

    // Create the streaming task
    final List<String> baseArgs = settingsService.getSetting(FFMPEG_BASE_ARGS, List.class);
    final FFmpeg ffmpeg = new FFmpeg(this.ffmpegExec, baseArgs);
    final FFmpegStreamTask streamTask = ffmpeg.getHlsStreamTask(transcodeRequest);

    // Add to collection
    streamingTasks.put(absolutePath, streamTask);
    // Return playlist file path
    return streamTask;
  }

  /** Cancels all streaming tasks running in the background */
  public void interruptAllStreamTasks() {
    ProcessHandle.allProcesses()
        .filter(p -> p.info().command().map(c -> c.contains("ffmpeg")).orElse(false))
        .forEach(ProcessHandle::destroyForcibly);
    streamingTasks.clear();
  }

  /**
   * Kills a task associated with the given directory, if there is one
   *
   * @param outputPath The path of the stream data
   */
  public void interruptStreamingTask(@NotNull final Path outputPath) throws InterruptedException {
    // Get absolute path for task key
    final Path absolutePath = outputPath.toAbsolutePath();

    // Get requested task
    final FFmpegStreamTask streamingTask = streamingTasks.get(absolutePath);
    if (streamingTask != null) {
      // kill task
      streamingTask.kill();
    }
    streamingTasks.remove(absolutePath);
  }

  /**
   * Returns the number of currently executing streaming tasks
   *
   * @return Number of streaming tasks
   */
  public int getStreamingTaskCount() {
    return streamingTasks.size();
  }

  /**
   * Wrap the FFprobe metadata method
   *
   * @param uri The URI of the audio/video file
   * @return An FFmpegMetadata object of the file's metadata, or null
   * @throws IOException I/O problem
   */
  @SuppressWarnings("unchecked cast")
  public FFmpegMetadata readFileMetadata(@NotNull final URI uri) throws IOException {
    List<String> baseArgs = settingsService.getSetting(FFPROBE_BASE_ARGS, List.class);
    FFprobe ffprobe = new FFprobe(this.ffprobeExec, baseArgs);
    return ffprobe.getFileMetadata(uri);
  }

  @Override
  public UUID getPluginId() {
    return UUID.fromString(pluginProperties.getId());
  }

  @Override
  public String getTitle() {
    return pluginProperties.getTitle();
  }

  @Override
  public String getDescription() {
    return pluginProperties.getDescription();
  }

  /**
   * Determines if there is a task streaming to the given directory
   *
   * @param absolutePath The path of the streaming task
   */
  private void checkTaskAlreadyExecuting(@NotNull final Path absolutePath) {
    // Check if a task is already working in path
    FFmpegStreamTask prevTask = streamingTasks.get(absolutePath);
    if (prevTask != null) {
      if (!prevTask.isAlive()) {
        // Kill zombie task & proceed
        try {
          prevTask.kill();
        } catch (InterruptedException ignore) {
        }
        streamingTasks.remove(absolutePath);
      } else {
        throw new IllegalThreadStateException(
            "FFmpeg has already started streaming to path: " + absolutePath);
      }
    }
  }
}
