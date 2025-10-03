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

package net.tomasbot.matchday.api.service.video;

import static net.tomasbot.matchday.api.controller.VideoStreamStatusController.VIDEO_STREAM_EMIT_ENDPOINT;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.tomasbot.matchday.api.controller.VideoStreamStatusController;
import net.tomasbot.matchday.api.controller.VideoStreamStatusController.VideoStreamStatusMessage;
import net.tomasbot.matchday.db.VideoFileRepository;
import net.tomasbot.matchday.db.VideoStreamLocatorRepo;
import net.tomasbot.matchday.model.video.SingleStreamLocator;
import net.tomasbot.matchday.model.video.VideoFile;
import net.tomasbot.matchday.model.video.VideoStreamLocator;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoStreamLocatorService {

  private final VideoStreamLocatorRepo streamLocatorRepo;
  private final VideoFileRepository videoFileRepository;
  private final VideoStreamStatusController streamStatusController;
  private final SimpMessagingTemplate messagingTemplate;

  @Value("${video-resources.playlist-name}")
  private String PLAYLIST_NAME;

  public VideoStreamLocatorService(
      VideoStreamLocatorRepo streamLocatorRepo,
      VideoFileRepository videoFileRepository,
      VideoStreamStatusController streamStatusController,
      SimpMessagingTemplate messagingTemplate) {
    this.streamLocatorRepo = streamLocatorRepo;
    this.videoFileRepository = videoFileRepository;
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
   * @param videoFileId The ID of the video file representation
   * @return The most recent stream locator for this video file, or Optional.empty();
   */
  public Optional<VideoStreamLocator> getStreamLocatorFor(@NotNull UUID videoFileId) {

    final List<VideoStreamLocator> streamLocators =
        streamLocatorRepo.getStreamLocatorsFor(videoFileId);
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
      @NotNull Path storageLocation, @NotNull VideoFile videoFile) {
    final UUID fileId = videoFile.getFileId();
    final Path playlistPath = storageLocation.resolve(fileId.toString()).resolve(PLAYLIST_NAME);
    final SingleStreamLocator locator = new SingleStreamLocator(playlistPath, videoFile);
    return streamLocatorRepo.saveAndFlush(locator);
  }

  @Transactional
  public VideoStreamLocator createStreamLocator(
      @NotNull Path storageLocation, @NotNull UUID videoFileId) {
    return videoFileRepository
        .findById(videoFileId)
        .map(videoFile -> createStreamLocator(storageLocation, videoFile))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Cannot create stream: no VideoFile with ID: " + videoFileId));
  }

  public void updateStreamLocator(@NotNull VideoStreamLocator streamLocator) {
    streamLocatorRepo.saveAndFlush(streamLocator);
    publishLocatorStatus(streamLocator);
  }

  /**
   * Delete the stream locator
   *
   * @param streamLocator The playlist to be deleted
   */
  @Transactional
  public void deleteStreamLocator(@NotNull VideoStreamLocator streamLocator) {
    streamLocatorRepo.deleteById(streamLocator.getStreamLocatorId());
    publishLocatorStatus(streamLocator);
  }

  public void publishLocatorStatus(@NotNull VideoStreamLocator streamLocator) {
    VideoFile videoFile = streamLocator.getVideoFile();
    if (videoFile == null) return;

    UUID videoFileId = videoFile.getFileId();
    VideoStreamStatusMessage message = streamStatusController.publishVideoStreamStatus(videoFileId);
    messagingTemplate.convertAndSend(VIDEO_STREAM_EMIT_ENDPOINT, message);
  }
}
