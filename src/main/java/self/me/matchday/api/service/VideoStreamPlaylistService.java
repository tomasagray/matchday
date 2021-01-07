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

package self.me.matchday.api.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.VideoStreamPlaylistRepo;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.VideoStreamLocator;
import self.me.matchday.model.VideoStreamPlaylist;
import self.me.matchday.util.Log;

import java.util.List;
import java.util.Optional;

@Service
public class VideoStreamPlaylistService {

  private static final String LOG_TAG = "VideoStreamPlaylistService";

  private final VideoStreamPlaylistRepo playlistRepo;
  private final PlaylistLocatorService playlistLocatorService;
  private final EventFileSelectorService eventFileSelectorService;

  @Autowired
  public VideoStreamPlaylistService(
      final VideoStreamPlaylistRepo playlistRepo,
      final PlaylistLocatorService playlistLocatorService,
      final EventFileSelectorService eventFileSelectorService) {

    this.playlistRepo = playlistRepo;
    this.playlistLocatorService = playlistLocatorService;
    this.eventFileSelectorService = eventFileSelectorService;
  }

  /**
   * Create a playlist of video streams. Directories for video data will be automatically created.
   *
   * @param fileSource The EventFileSource from which the stream will be created
   * @return The playlist of video streams
   */
  @Transactional
  public VideoStreamPlaylist createVideoStreamPlaylist(@NotNull final EventFileSource fileSource) {

    // Get list of "best" EventFiles for each event part
    final List<EventFile> playlistFiles = eventFileSelectorService.getPlaylistFiles(fileSource);
    // Create stream playlist
    final VideoStreamPlaylist streamPlaylist = new VideoStreamPlaylist(fileSource);
    // Create locator for each file stream
    playlistFiles.forEach(
        eventFile -> {
          // Create storage path for each task
          final VideoStreamLocator playlistLocator =
              playlistLocatorService.createNewPlaylistLocator(fileSource, eventFile);
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
   * @param fileSrcId The ID of the EventFileSource this playlist was created from
   * @return An Optional containing the most recent playlist
   */
  public Optional<VideoStreamPlaylist> getVideoStreamPlaylist(@NotNull final String fileSrcId) {

    final List<VideoStreamPlaylist> playlists = playlistRepo.fetchPlaylistsForFileSrc(fileSrcId);
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
  public void deleteVideoStreamPlaylist(@NotNull final VideoStreamPlaylist streamPlaylist) {

    // Nullify stream locator EventFiles so an FK constraint exception is not thrown;
    // EventFiles are also referenced by EventFileSources
    streamPlaylist.getStreamLocators().forEach(streamLocator -> streamLocator.setEventFile(null));
    playlistRepo.delete(streamPlaylist);
  }
}
