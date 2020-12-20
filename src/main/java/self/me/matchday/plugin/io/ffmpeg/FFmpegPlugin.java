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
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.me.matchday.plugin.Plugin;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

@Component
public class FFmpegPlugin implements Plugin {

  private static final String LOG_TAG = "FFmpegPlugin";

  private final FFmpegPluginProperties pluginProperties;
  private final FFmpeg ffmpeg;
  private final FFprobe ffprobe;
  private final Hashtable<Path, FFmpegStreamTask> streamingTasks = new Hashtable<>();

  @Autowired
  public FFmpegPlugin(@NotNull final FFmpegPluginProperties pluginProperties) {

    this.pluginProperties = pluginProperties;
    // Create executable instances
    ffmpeg = new FFmpeg(pluginProperties.getFFmpegLocation());
    ffprobe = new FFprobe(pluginProperties.getFFprobeLocation());
  }

  /**
   * Create a concatenated HLS stream from a given collection of URIs
   *
   * @param uris URI pointers to video data
   * @param storageLocation The output location for stream data
   * @return The path of the playlist file produced by FFMPEG
   */
  public FFmpegStreamTask streamUris(@NotNull final List<URI> uris, @NotNull final Path storageLocation) {

    // Get absolute path for task key
    final Path absolutePath = storageLocation.toAbsolutePath();
    // Check if a task is already working in path
    FFmpegStreamTask prevTask = isTaskAlreadyExecuting(absolutePath);
    if (prevTask != null) {
      return prevTask;
    }

    // Create the streaming task
    final FFmpegStreamTask streamTask = ffmpeg.getHlsStreamTask(uris, storageLocation);
    // Add to collection
    streamingTasks.put(absolutePath, streamTask);
    // Start streaming task
    streamTask.start();

    // Return playlist file path
    return streamTask;
  }

  /**
   * Create an HLS stream from a single URI
   *
   * @param uri The file resource pointer
   * @param storageLocation The location on disk to store stream data
   * @return The streaming task
   */
  public FFmpegStreamTask streamUri(@NotNull final URI uri, @NotNull final Path storageLocation) {

    // Get absolute path for task key
    final Path absolutePath = storageLocation.toAbsolutePath();
    // Check if a task is already working in path
    FFmpegStreamTask prevTask = isTaskAlreadyExecuting(absolutePath);
    if (prevTask != null) {
      return prevTask;
    }

    // Create the streaming task
    final FFmpegStreamTask streamTask = ffmpeg.getHlsStreamTask(uri, storageLocation);
    // Add to collection
    Log.i(LOG_TAG, "Adding streaming task to: " + absolutePath);
    streamingTasks.put(absolutePath, streamTask);

    // Return playlist file path
    return streamTask;
  }

  /**
   * Determines if there is a task streaming to the given directory
   *
   * @param absolutePath The path of the streaming task
   * @return The task that is executing in the given directory, or null if none found
   */
  private @Nullable FFmpegStreamTask isTaskAlreadyExecuting(Path absolutePath) {
    final FFmpegStreamTask prevTask = streamingTasks.get(absolutePath);
    if (prevTask != null) {
      if (prevTask.isAlive()) {
        // Task is already present and alive; abort
        Log.i(LOG_TAG, String.format("Streaming task already begun for path: %s", absolutePath));
        return prevTask;
      } else {
        // Previous task has finished; remove from list & continue
        streamingTasks.remove(absolutePath);
      }
    }
    return null;
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
          LOG_TAG, String.format("Streaming task to [%s] killed: %s", absolutePath, processKilled));
      if (processKilled) {
        // remove from task list
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
