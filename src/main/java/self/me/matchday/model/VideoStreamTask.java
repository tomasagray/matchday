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

package self.me.matchday.model;

import lombok.Builder;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Configurable;
import self.me.matchday.api.service.EventFileService;
import self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin;

import java.net.URI;
import java.nio.file.Path;

@Configurable
@Builder
public class VideoStreamTask extends Thread {

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

    final EventFile refreshedEventFile = eventFileService.refreshEventFile(this.eventFile, false);
    // Get link to video data
    final URI videoDataLink = refreshedEventFile.getInternalUrl().toURI();
    // Start stream
    final Thread streamTask = ffmpegPlugin.streamUris(playlistPath, videoDataLink);
    streamTask.start();
    // Wait for stream task to finish
    streamTask.join();
  }
}
