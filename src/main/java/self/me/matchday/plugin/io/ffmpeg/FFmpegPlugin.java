/*
 * Copyright (c) 2020.
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

package self.me.matchday.plugin.io.ffmpeg;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.me.matchday.plugin.Plugin;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;

@Component
public class FFmpegPlugin implements Plugin {

  private static final String LOG_TAG = "FFmpegPlugin";

  private final FFmpegPluginProperties pluginProperties;
  private final FFmpeg ffmpeg;
  private final FFprobe ffprobe;
  private final Map<Path, FFmpegStreamTask> streamingTasks = new LinkedHashMap<>();

  @Autowired
  public FFmpegPlugin(@NotNull final FFmpegPluginProperties pluginProperties) {

    this.pluginProperties = pluginProperties;
    // Create executable instances
    this.ffmpeg = new FFmpeg(pluginProperties.getFFmpegLocation());
    this.ffprobe = new FFprobe(pluginProperties.getFFprobeLocation());
  }

  /**
   * Create an HLS stream from a given collection of URIs
   *
   * @param uris URI pointers to video data
   * @param playlistPath The output location for stream data
   * @return The path of the playlist file produced by FFMPEG
   */
  public FFmpegStreamTask streamUris(
      @NotNull final Path playlistPath, @NotNull final URI... uris) {

    // Get absolute path for task key
    final Path absolutePath = playlistPath.toAbsolutePath();
    checkTaskAlreadyExecuting(absolutePath);
    // Create the streaming task
    final FFmpegStreamTask streamTask = ffmpeg.getHlsStreamTask(playlistPath, uris);
    // Add to collection
    streamingTasks.put(absolutePath, streamTask);
    // Return playlist file path
    return streamTask;
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
        prevTask.kill();
        streamingTasks.remove(absolutePath);
      } else {
        throw new IllegalThreadStateException(
                "FFmpeg has already started streaming to path: " + absolutePath);
      }
    }
  }

  /** Cancels all streaming tasks running in the background */
  public void interruptAllStreamTasks() {

    // kill each task
    streamingTasks.forEach(
        (pid, ffmpegTask) -> {
          Log.i(LOG_TAG, "Killing streaming task with thread ID: " + pid);
          ffmpegTask.kill();
        });
    // clear task list
    streamingTasks.clear();
  }

  /**
   * Kills a task associated with the given directory, if there is one
   *
   * @param outputPath The path of the stream data
   */
  public void interruptStreamingTask(@NotNull final Path outputPath) {

    // Get absolute path for task key
    final Path absolutePath = outputPath.toAbsolutePath();
    // Get requested task
    final FFmpegStreamTask streamingTask = streamingTasks.get(absolutePath);
    if (streamingTask != null) {
      // kill task
      Log.i(LOG_TAG, "Killing streaming task to file: " + absolutePath);
      final boolean processKilled = streamingTask.kill();
      Log.i(
          LOG_TAG, String.format("Streaming task to [%s] killed? %s", absolutePath, processKilled));
      if (processKilled) {
        streamingTasks.remove(absolutePath);
      }
    }
    Log.i(LOG_TAG, "No task found for output file: " + absolutePath);
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
  public FFmpegMetadata readFileMetadata(@NotNull final URI uri) throws IOException {
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
}
