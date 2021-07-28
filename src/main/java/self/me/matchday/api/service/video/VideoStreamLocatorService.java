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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.VideoStreamLocatorRepo;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.video.SingleStreamLocator;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.util.Log;
import self.me.matchday.util.RecursiveDirectoryDeleter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
public class VideoStreamLocatorService {

  private static final String PLAYLIST_NAME = "playlist.m3u8"; // todo - move to config
  private static final String LOG_TAG = "PlaylistLocatorService";

  private final VideoStreamLocatorRepo streamLocatorRepo;

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
   * Find the latest video stream locator for a given video file
   *
   * @param eventFile The video file representation
   * @return The most recent stream locator for this video file, or Optional.empty();
   */
  public Optional<VideoStreamLocator> getStreamLocatorFor(@NotNull final EventFile eventFile) {

    final List<VideoStreamLocator> streamLocators =
        streamLocatorRepo.getStreamLocatorsFor(eventFile);
    if (streamLocators.isEmpty()) {
      return Optional.empty();
    }
    // return first (latest) locator
    return Optional.of(streamLocators.get(0));
  }

  /**
   * Creates a new VideoStreamLocator and saves it to database.
   *
   * @param storageLocation The Path of the data directory
   * @param eventFile Video data for the stream
   * @return The newly created VideoStreamLocator
   */
  @Transactional
  public VideoStreamLocator createStreamLocator(
      @NotNull final Path storageLocation, @NotNull final EventFile eventFile) {

    // Create streaming storage path
    final Path playlistPath =
        storageLocation.resolve(eventFile.getEventFileId()).resolve(PLAYLIST_NAME);
    // Create playlist locator
    final VideoStreamLocator streamLocator = new SingleStreamLocator(playlistPath, eventFile);
    // Save locator to database & return
    Log.i(LOG_TAG, "Saving stream locator: " + streamLocatorRepo.saveAndFlush(streamLocator));
    return streamLocator;
  }

  @Transactional
  public void saveStreamLocator(@NotNull final VideoStreamLocator streamLocator) {
    streamLocatorRepo.saveAndFlush(streamLocator);
  }

  /**
   * Delete the playlist locator
   *
   * @param streamLocator The playlist to be deleted
   */
  @Transactional
  public void deleteStreamLocator(@NotNull final VideoStreamLocator streamLocator) {
    streamLocatorRepo.delete(streamLocator);
  }

  @Transactional
  public void deleteStreamLocatorWithData(@NotNull final VideoStreamLocator streamLocator)
      throws IOException {

    final Path playlistPath = streamLocator.getPlaylistPath();
    if (!playlistPath.toFile().isFile()) {
      throw new IllegalArgumentException("VideoStreamLocator does not refer to a file");
    }

    final Path streamDataDir = playlistPath.getParent();
    Files.walkFileTree(streamDataDir, new RecursiveDirectoryDeleter());
    Log.i(
        LOG_TAG,
        "Successfully deleted local data associated with Video Stream Locator: " + streamLocator);
  }
}
