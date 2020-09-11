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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.me.matchday.plugin.Plugin;

@Component
public class FFmpegPlugin implements Plugin {

  private final FFmpegPluginProperties pluginProperties;
  private final FFmpeg ffmpeg;
  private final FFprobe ffprobe;
  private final ThreadGroup threadGroup;

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
  public Path streamUris(@NotNull final List<URI> uris, @NotNull final Path storageLocation) {

    // Create the streaming task
    final FFmpegTask streamTask = ffmpeg.getHlsStreamTask(uris, storageLocation);
    // Get playlist file location
    Path playlistFile = streamTask.getOutputFile();

    // Create thread for task
    final Thread thread = new Thread(threadGroup, streamTask);
    // Start streaming task
    thread.start();

    // Return playlist file path
    return playlistFile;
  }

  /**
   * Cancels all streaming tasks running in the background
   *
   */
  public void interruptStreamTasks() {
    // todo - implement streaming interruption
    threadGroup.interrupt();
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
