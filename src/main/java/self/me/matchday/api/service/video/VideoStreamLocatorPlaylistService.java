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

package self.me.matchday.api.service.video;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.api.service.VideoFileSelectorService;
import self.me.matchday.db.VideoStreamLocatorPlaylistRepo;
import self.me.matchday.model.video.VideoFilePack;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;
import self.me.matchday.util.Log;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@PropertySource("classpath:video.properties")
public class VideoStreamLocatorPlaylistService {

  private static final String LOG_TAG = "VideoStreamPlaylistService";

  private final VideoStreamLocatorPlaylistRepo playlistRepo;
  private final VideoStreamLocatorService videoStreamLocatorService;
  private final VideoFileSelectorService videoFileSelectorService;

  @Value("${video-resources.file-storage-location}")
  private Path fileStorageLocation;

  @Autowired
  public VideoStreamLocatorPlaylistService(
      final VideoStreamLocatorPlaylistRepo playlistRepo,
      final VideoStreamLocatorService videoStreamLocatorService,
      final VideoFileSelectorService videoFileSelectorService) {

    this.playlistRepo = playlistRepo;
    this.videoStreamLocatorService = videoStreamLocatorService;
    this.videoFileSelectorService = videoFileSelectorService;
  }

  /**
   * Retrieve all video stream playlists.
   *
   * @return A List of all video stream playlists in the database
   */
  public List<VideoStreamLocatorPlaylist> getAllVideoStreamPlaylists() {
    return playlistRepo.findAll();
  }

  /**
   * Create a playlist of video streams. Directories for video data will be automatically created.
   *
   * @param fileSource The VideoFileSource from which the stream will be created
   * @return The playlist of video streams
   */
  @Transactional
  public VideoStreamLocatorPlaylist createVideoStreamPlaylist(
      @NotNull final VideoFileSource fileSource) {

    // Get list of "best" VideoFiles for each event part
    final VideoFilePack playlistFiles = videoFileSelectorService.getPlaylistFiles(fileSource);
    if (playlistFiles == null || playlistFiles.size() == 0) {
      final String msg =
          String.format(
              "Could not create VideoStreamLocatorPlaylist from file source: %s; has no video files!",
              fileSource);
      throw new IllegalArgumentException(msg);
    }

    // Create storage path
    final UUID fileSrcId = fileSource.getFileSrcId();
    final Path storageLocation = fileStorageLocation.resolve(fileSrcId.toString());
    // Create stream playlist
    final VideoStreamLocatorPlaylist streamPlaylist =
        new VideoStreamLocatorPlaylist(fileSource, storageLocation);

    // Create locator for each file stream
    playlistFiles.forEach(
        (title, videoFile) -> {
          // Create storage path for each task
          final VideoStreamLocator playlistLocator =
              videoStreamLocatorService.createStreamLocator(storageLocation, videoFile);
          // Add  playlist locator to VideoStreamPlaylist
          streamPlaylist.addStreamLocator(playlistLocator);
        });
    // Save VideoStreamPlaylist & return
    Log.i(LOG_TAG, "Saved VideoStreamPlaylist: " + playlistRepo.saveAndFlush(streamPlaylist));
    return streamPlaylist;
  }

  /**
   * Retrieve the most recently created playlist from the database
   *
   * @param fileSrcId The ID of the VideoFileSource this playlist was created from
   * @return An Optional containing the most recent playlist
   */
  public Optional<VideoStreamLocatorPlaylist> getVideoStreamPlaylistFor(
      @NotNull final UUID fileSrcId) {

    final List<VideoStreamLocatorPlaylist> playlists =
        playlistRepo.fetchPlaylistsForFileSrc(fileSrcId);
    if (playlists != null && playlists.size() > 0) {
      // Return most recent playlist
      return Optional.of(playlists.get(0));
    }
    // Nothing found
    return Optional.empty();
  }

  /**
   * Delete a video stream playlist & it's associated stream locators from database
   *
   * @param streamPlaylist The playlist to be deleted
   */
  public void deleteVideoStreamPlaylist(@NotNull final VideoStreamLocatorPlaylist streamPlaylist) {
    playlistRepo.delete(streamPlaylist);
  }
}
