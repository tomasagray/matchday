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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.VideoStreamLocatorRepo;
import self.me.matchday.model.*;
import self.me.matchday.util.Log;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
public class VideoStreamLocatorService {

  private static final String PLAYLIST_NAME = "playlist.m3u8";
  private static final String LOG_TAG = "PlaylistLocatorService";

  // Repositories
  private final VideoStreamLocatorRepo streamLocatorRepo;
  // Configuration
  @Value("${video-resources.file-storage-location}")
  private String fileStorageLocation;

  @Autowired
  public VideoStreamLocatorService(final VideoStreamLocatorRepo streamLocatorRepo) {

    this.streamLocatorRepo = streamLocatorRepo;
  }

  /**
   * Get all playlist locators from database
   *
   * @return A List of VideoStreamPlaylistLocators
   */
  public List<VideoStreamLocator> getAllStreamLocators() {
    return streamLocatorRepo.findAll();
  }

  /**
   * Find a VideoStreamLocator in the database.
   *
   * @param streamLocatorId The ID of the video stream data locator
   * @return An Optional of the playlist locator
   */
  public Optional<VideoStreamLocator> getStreamLocator(final Long streamLocatorId) {

    return streamLocatorRepo.findById(streamLocatorId);
  }

  /**
   * Creates a new VideoStreamLocator and saves it to database.
   *
   * @param fileSource The EventFileSource this video stream comes from
   * @param eventFile Video data for the stream
   * @return The newly created VideoStreamLocator
   */
  @Transactional
  public VideoStreamLocator createStreamLocator(
      @NotNull final EventFileSource fileSource, @NotNull final EventFile eventFile) {

    // Create playlist IDs
    final String fileSrcId = fileSource.getEventFileSrcId();
    final String eventFileId = MD5String.fromData(eventFile.getExternalUrl());

    // Create streaming storage path
    final Path playlistPath = Path.of(fileStorageLocation, fileSrcId, eventFileId, PLAYLIST_NAME);
    // Create playlist locator
    final VideoStreamLocator playlistLocator = new SingleStreamLocator(playlistPath, eventFile);
    // Save locator to database & return
    Log.i(LOG_TAG, "Saving playlist locator: " + streamLocatorRepo.saveAndFlush(playlistLocator));
    return playlistLocator;
  }

  /**
   * Delete the playlist locator
   *
   * @param streamLocator The playlist to be deleted
   */
  public void deleteStreamLocator(@NotNull final VideoStreamLocator streamLocator) {
    streamLocatorRepo.delete(streamLocator);
  }
}
