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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.api.controller.VideoStreamStatusController;
import self.me.matchday.db.VideoStreamLocatorRepo;
import self.me.matchday.model.video.SingleStreamLocator;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.util.RecursiveDirectoryDeleter;

@Service
public class VideoStreamLocatorService {

  @Value("${video-resources.playlist-name}")
  private String PLAYLIST_NAME;

  private final VideoStreamLocatorRepo streamLocatorRepo;
  private final VideoStreamStatusController streamStatusController;
  private final SimpMessagingTemplate messagingTemplate;

  public VideoStreamLocatorService(
      final VideoStreamLocatorRepo streamLocatorRepo,
      VideoStreamStatusController streamStatusController,
      SimpMessagingTemplate messagingTemplate) {
    this.streamLocatorRepo = streamLocatorRepo;
    this.streamStatusController = streamStatusController;
    this.messagingTemplate = messagingTemplate;
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
   * @param videoFile The video file representation
   * @return The most recent stream locator for this video file, or Optional.empty();
   */
  public Optional<VideoStreamLocator> getStreamLocatorFor(@NotNull final VideoFile videoFile) {

    final List<VideoStreamLocator> streamLocators =
        streamLocatorRepo.getStreamLocatorsFor(videoFile.getFileId());
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
   * @param videoFile Video data for the stream
   * @return The newly created VideoStreamLocator
   */
  @Transactional
  public VideoStreamLocator createStreamLocator(
      @NotNull final Path storageLocation, @NotNull final VideoFile videoFile) {

    final UUID fileId = videoFile.getFileId();
    final Path playlistPath = storageLocation.resolve(fileId.toString()).resolve(PLAYLIST_NAME);
    return streamLocatorRepo.saveAndFlush(new SingleStreamLocator(playlistPath, videoFile));
  }

  @Transactional
  public void updateStreamLocator(@NotNull final VideoStreamLocator streamLocator) {
    streamLocatorRepo.saveAndFlush(streamLocator);
    final VideoFile videoFile = streamLocator.getVideoFile();
    messagingTemplate.convertAndSend(
        VideoStreamStatusController.EMIT_ENDPOINT,
        streamStatusController.publishVideoStreamStatus(videoFile.getFileId()));
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

  private static void deleteVideoDataFromDisk(
      @NotNull VideoStreamLocator streamLocator, @NotNull Path playlistPath) throws IOException {

    final File playlistFile = playlistPath.toFile();
    // if the file doesn't exist, just return
    if (!playlistFile.exists()) {
      return;
    }

    if (!playlistFile.isFile()) {
      final String msg =
          String.format(
              "VideoStreamLocator: %s does not refer to a file! Will not delete",
              streamLocator.getStreamLocatorId());
      throw new IllegalArgumentException(msg);
    }

    // delete the data
    final Path streamDataDir = playlistPath.getParent();
    Files.walkFileTree(streamDataDir, new RecursiveDirectoryDeleter());
  }

  @Transactional
  public void deleteStreamLocatorWithData(@NotNull final VideoStreamLocator streamLocator)
      throws IOException {

    final Path playlistPath = streamLocator.getPlaylistPath();
    deleteStreamLocator(streamLocator);
    deleteVideoDataFromDisk(streamLocator, playlistPath);
  }
}
