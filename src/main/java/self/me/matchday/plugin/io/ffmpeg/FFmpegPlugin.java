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
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

@Component
public class FFmpegPlugin implements Plugin {

  private static final String LOG_TAG = "FFmpegPlugin";

  private final FFmpegPluginProperties pluginProperties;
  private final FFmpeg ffmpeg;
  private final FFprobe ffprobe;

  private final ThreadGroup threadGroup;
  private final Hashtable<String, FFmpegTask> streamingTasks = new Hashtable<>();

  @Autowired
  public FFmpegPlugin(@NotNull final FFmpegPluginProperties pluginProperties) {

    this.pluginProperties = pluginProperties;
    // Create executable instances
    ffmpeg = new FFmpeg(pluginProperties.getFFmpegLocation());
    ffprobe = new FFprobe(pluginProperties.getFFprobeLocation());
    // Task container
    threadGroup = new ThreadGroup("ffmpeg");
  }

  /**
   * Create an HLS stream from a given collection of URIs
   *
   * @param uris URI pointers to video data
   * @param storageLocation The output location for stream data
   * @return The path of the playlist file produced by FFMPEG
   */
  public FFmpegTask streamUris(@NotNull final List<URI> uris, @NotNull final Path storageLocation) {

    // Create the streaming task
    final FFmpegTask streamTask = ffmpeg.getHlsStreamTask(uris, storageLocation);

    // Create thread for task
    final Thread thread = new Thread(threadGroup, streamTask);
    // Start streaming task
    thread.start();
    // Add to collection
    streamingTasks.put(streamTask.getOutputFile().toString(), streamTask);

    // Return playlist file path
    return streamTask;
  }

  /**
   * Cancels all streaming tasks running in the background
   *
   */
  public void interruptAllStreamTasks() {

    // kill each task
    streamingTasks.forEach(
        (pid, ffmpegTask) -> {
          Log.i(LOG_TAG, "Killing streaming task with thread ID: " + pid);
          ffmpegTask.kill();
        });
    // clear task list
    streamingTasks.clear();
    threadGroup.interrupt();
  }

  public void interruptStreamingTask(@NotNull final String outputPath) {

    // Get requested task
    final FFmpegTask streamingTask = streamingTasks.get(outputPath);
    if (streamingTask != null) {
      // kill task
      Log.i(LOG_TAG, "Killing streaming task to file: " + outputPath);
      final boolean processKilled = streamingTask.kill();
      Log.i(LOG_TAG, String.format("Streaming task to [%s] killed: %s", outputPath, processKilled));
      if (processKilled) {
        // remove from task list
        streamingTasks.remove(outputPath);
      }
    }

    Log.i(LOG_TAG, "No task found for output file: " + outputPath);
  }

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

    return
        ffprobe.getFileMetadata(uri);
  }

  @Override
  public UUID getPluginId() {
    return
        UUID.fromString(pluginProperties.getId());
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
