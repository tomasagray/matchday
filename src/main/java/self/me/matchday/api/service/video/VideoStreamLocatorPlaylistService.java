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

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.api.service.SettingsService;
import self.me.matchday.db.VideoStreamLocatorPlaylistRepo;
import self.me.matchday.model.video.VideoFilePack;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;

@Service
@PropertySource("classpath:video.properties")
@Transactional
public class VideoStreamLocatorPlaylistService {

  private final VideoStreamLocatorPlaylistRepo playlistRepo;
  private final VideoStreamLocatorService locatorService;
  private final VideoFileSelectorService videoFileSelectorService;
  private final SettingsService settingsService;

  public VideoStreamLocatorPlaylistService(
      final VideoStreamLocatorPlaylistRepo playlistRepo,
      final VideoStreamLocatorService locatorService,
      final VideoFileSelectorService videoFileSelectorService,
      SettingsService settingsService) {

    this.playlistRepo = playlistRepo;
    this.locatorService = locatorService;
    this.videoFileSelectorService = videoFileSelectorService;
    this.settingsService = settingsService;
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
  public VideoStreamLocatorPlaylist createVideoStreamPlaylist(
      @NotNull final VideoFileSource fileSource) {

    final VideoFilePack playlistFiles = videoFileSelectorService.getPlaylistFiles(fileSource);
    if (playlistFiles == null || playlistFiles.size() == 0) {
      throw new EmptyVideoFileSourceException(fileSource);
    }

    final Path fileStorageLocation = settingsService.getSettings().getVideoStorageLocation();
    final UUID fileSrcId = fileSource.getFileSrcId();
    final Path storageLocation = fileStorageLocation.resolve(fileSrcId.toString());
    final VideoStreamLocatorPlaylist streamPlaylist =
        new VideoStreamLocatorPlaylist(fileSource, storageLocation);

    playlistFiles.forEachVideoFile(
        (title, videoFile) -> {
          final VideoStreamLocator playlistLocator =
              locatorService.createStreamLocator(storageLocation, videoFile);
          streamPlaylist.addStreamLocator(playlistLocator);
        });
    return playlistRepo.saveAndFlush(streamPlaylist);
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

  public Optional<VideoStreamLocatorPlaylist> getVideoStreamPlaylistContaining(
      @NotNull Long locatorId) {
    return playlistRepo.fetchPlaylistContaining(locatorId);
  }

  /**
   * Delete a video stream playlist & it's associated stream locators from database
   *
   * @param playlist The playlist to be deleted
   */
  public void deleteVideoStreamPlaylist(@NotNull final VideoStreamLocatorPlaylist playlist) {
    playlistRepo.delete(playlist);
    playlist.getStreamLocators().forEach(locatorService::publishLocatorStatus);
  }
}
