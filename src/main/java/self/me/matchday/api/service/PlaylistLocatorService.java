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

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.db.VideoPlaylistLocatorRepo;
import self.me.matchday.model.VideoStreamPlaylistLocator;
import self.me.matchday.model.VideoStreamPlaylistLocator.VideoStreamPlaylistId;
import self.me.matchday.util.Log;

@Service
public class PlaylistLocatorService {

  private static final String LOG_TAG = "PlaylistLocatorService";

  private final VideoPlaylistLocatorRepo playlistLocatorRepo;

  @Autowired
  public PlaylistLocatorService(final VideoPlaylistLocatorRepo playlistLocatorRepo) {

    this.playlistLocatorRepo = playlistLocatorRepo;
  }

  /**
   * Get all playlist locators from database
   *
   * @return A List of VideoStreamPlaylistLocators
   */
  public List<VideoStreamPlaylistLocator> getAllPlaylistLocators() {

    return playlistLocatorRepo.findAll();
  }

  /**
   * Find a VideoStreamPlaylistLocator in the database.
   *
   * @param eventId   The ID of the Event for this playlist
   * @param fileSrcId The File Source ID for this playlist
   * @return An Optional of the playlist locator
   */
  public Optional<VideoStreamPlaylistLocator> getPlaylistLocator(@NotNull final String eventId,
      @NotNull final UUID fileSrcId) {

    // Create an ID to find the playlist
    final VideoStreamPlaylistId playlistId = new VideoStreamPlaylistId(eventId, fileSrcId);
    return
        playlistLocatorRepo.findById(playlistId);
  }

  /**
   * Creates a new VideoStreamPlaylistLocator and saves it to database.
   *
   * @param eventId      The ID of the Event for this playlist
   * @param fileSrcId    The ID of the File Source for this playlist
   * @param playlistPath The path to the playlist file
   * @return The newly created VideoStreamPlaylistLocator
   */
  public VideoStreamPlaylistLocator createNewPlaylistLocator(@NotNull final String eventId,
      @NotNull final UUID fileSrcId, @NotNull final Path playlistPath) {

    // Create locator ID
    final VideoStreamPlaylistId playlistId = new VideoStreamPlaylistId(eventId, fileSrcId);
    // Create playlist locator
    final VideoStreamPlaylistLocator playlistLocator =
        new VideoStreamPlaylistLocator(playlistId, playlistPath);
    // Save locator to database
    Log.i(LOG_TAG, "Saving playlist locator: " + playlistLocatorRepo.save(playlistLocator));

    return playlistLocator;
  }

  /**
   * Delete the playlist locator based on the given IDs
   *
   * @param playlistId The ID of the playlist to be deleted
   */
  public void deletePlaylistLocator(@NotNull final VideoStreamPlaylistId playlistId) {
    // Delete playlist locator
    playlistLocatorRepo.deleteById(playlistId);
  }
}
