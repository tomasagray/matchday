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

package self.me.matchday.api.service;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Service;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.VideoStreamLocator;
import self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import self.me.matchday.util.Log;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
class VideoStreamManager {

  private static final String LOG_TAG = "VideoStreamManager";

  private final EventFileService eventFileService;
  private final FFmpegPlugin ffmpegPlugin;
  private final ExecutorService executorService = Executors.newFixedThreadPool(4);

  @Autowired
  public VideoStreamManager(
      final EventFileService eventFileService, final FFmpegPlugin ffmpegPlugin) {

    this.eventFileService = eventFileService;
    this.ffmpegPlugin = ffmpegPlugin;
  }

  void startVideoStreamTask(@NotNull final VideoStreamLocator streamLocator) {

    // Setup streaming job
    final Path playlistPath = streamLocator.getPlaylistPath();
    final EventFile eventFile = streamLocator.getEventFile();
    // Create streaming task
    final VideoStreamTask streamTask =
        VideoStreamTask.builder()
            .dependencies(eventFileService, ffmpegPlugin)
            .eventFile(eventFile)
            .storageLocation(playlistPath)
            .build();
    // Submit task to job processor
    Log.i(LOG_TAG, "Starting stream task to: " + streamTask.getPlaylistPath());
    executorService.execute(streamTask);
  }

  @Configurable
  static class VideoStreamTask extends Thread {

    /** Builder class for the VideoStreamTask class */
    static class VideoStreamTaskBuilder {

      private EventFileService eventFileService;
      private FFmpegPlugin ffmpegPlugin;
      private EventFile eventFile;
      private Path storageLocation;

      VideoStreamTaskBuilder dependencies(
          final EventFileService eventFileService, final FFmpegPlugin ffmpegPlugin) {
        this.eventFileService = eventFileService;
        this.ffmpegPlugin = ffmpegPlugin;
        return this;
      }

      VideoStreamTaskBuilder eventFile(final EventFile eventFile) {
        this.eventFile = eventFile;
        return this;
      }

      VideoStreamTaskBuilder storageLocation(final Path storageLocation) {
        this.storageLocation = storageLocation;
        return this;
      }

      VideoStreamTask build() {
        return new VideoStreamTask(eventFileService, ffmpegPlugin, eventFile, storageLocation);
      }
    }

    static VideoStreamTaskBuilder builder() {
      return new VideoStreamTaskBuilder();
    }

    // Dependencies
    private final EventFileService eventFileService;
    private final FFmpegPlugin ffmpegPlugin;
    // Data fields
    private final EventFile eventFile;
    private final Path playlistPath;

    private VideoStreamTask(
        final EventFileService eventFileService,
        final FFmpegPlugin ffmpegPlugin,
        final EventFile eventFile,
        final Path playlistPath) {
      this.eventFileService = eventFileService;
      this.ffmpegPlugin = ffmpegPlugin;
      this.eventFile = eventFile;
      this.playlistPath = playlistPath;
    }

    public Path getPlaylistPath() {
      return this.playlistPath;
    }

    @SneakyThrows
    @Override
    public void run() {

      final EventFile refreshedEventFile =
              eventFileService.refreshEventFile(this.eventFile, false);
      // Get link to video data
      final URI videoDataLink = refreshedEventFile.getInternalUrl().toURI();
      // Start stream
      final Thread streamTask = ffmpegPlugin.streamUri(videoDataLink, playlistPath);
      streamTask.start();
      // Wait for stream task to finish
      streamTask.join();
    }
  }
}
