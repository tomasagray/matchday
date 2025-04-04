/*
 * Copyright (c) 2023.
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.video.*;
import net.tomasbot.matchday.model.video.StreamJobState.JobStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoStreamingService {

  private final VideoFileSelectorService selectorService;
  private final VideoStreamManager videoStreamManager;
  private final VideoStreamLocatorPlaylistService playlistService;
  private final VideoStreamLocatorService locatorService;
  private final VideoSourceService videoSourceService;

  public VideoStreamingService(
      VideoFileSelectorService selectorService,
      VideoStreamManager videoStreamManager,
      VideoStreamLocatorPlaylistService playlistService,
      VideoStreamLocatorService locatorService,
      VideoSourceService videoSourceService) {
    this.selectorService = selectorService;
    this.videoStreamManager = videoStreamManager;
    this.playlistService = playlistService;
    this.locatorService = locatorService;
    this.videoSourceService = videoSourceService;
  }

  public List<VideoStreamLocatorPlaylist> fetchAllPlaylists() {
    return playlistService.getAllVideoStreamPlaylists();
  }

  public List<VideoStreamLocator> fetchAllVideoStreamLocators() {
    return locatorService.getAllStreamLocators();
  }

  public Optional<VideoPlaylist> getBestVideoStreamPlaylist(@NotNull Event event) {
    final Optional<VideoStreamLocatorPlaylist> existingStream = findExistingStream(event);
    final VideoFileSource fileSource =
        existingStream.isPresent()
            ? existingStream.get().getFileSource()
            : selectorService.getBestFileSource(event);
    return beginStreamingVideo(event, fileSource.getFileSrcId());
  }

  /**
   * Determine if this Event has already been streamed, as indicated by the presence of a matching
   * VideoStreamLocatorPlaylist in the database.
   *
   * @param event The Event to query for
   * @return An Optional which will contain the playlist, or not
   */
  public @NotNull Optional<VideoStreamLocatorPlaylist> findExistingStream(@NotNull Event event) {
    return event.getFileSources().stream()
        .map(VideoFileSource::getFileSrcId)
        .map(playlistService::getVideoStreamPlaylistFor)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  public Optional<VideoPlaylist> beginStreamingVideo(
      @NotNull Event event, @NotNull UUID fileSrcId) {
    return getOrCreateVideoStreamPlaylist(event, fileSrcId)
        .map(
            playlist -> {
              Collection<VideoStreamLocator> streamJobs = getStreamJobs(playlist);
              videoStreamManager.queueStreamJobs(streamJobs);
              return Optional.of(playlist);
            })
        .orElseThrow(
            () ->
                new IllegalStateException("Could not start stream for file source: " + fileSrcId));
  }

  public Optional<VideoPlaylist> downloadVideoStream(
      @NotNull Event event, @NotNull UUID fileSrcId, @NotNull UUID videoFileId) {
    Optional<VideoPlaylist> playlistOptional = getOrCreateVideoStreamPlaylist(event, fileSrcId);

    if (playlistOptional.isPresent()) {
      final VideoPlaylist videoPlaylist = playlistOptional.get();
      final VideoStreamLocator streamLocator =
          videoPlaylist.getLocatorIds().keySet().stream()
              .map(locatorService::getStreamLocator)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .filter(locator -> locator.getVideoFile().getFileId().equals(videoFileId))
              .findFirst()
              .orElse(new SingleStreamLocator())
          /*.orElseThrow(
          () ->
              new IllegalArgumentException(
                  "No Video Stream was located for the given parameters"))*/ ;

      videoStreamManager.queueStreamJob(streamLocator);
    }
    return playlistOptional;
  }

  /**
   * Get the most recent video stream playlist for the given file source
   *
   * @param fileSrcId The ID of the VideoFileSource
   * @return An Optional containing the playlist, if one was found; empty indicates one is being
   *     created
   */
  private Optional<VideoPlaylist> getOrCreateVideoStreamPlaylist(
      @NotNull Event event, @NotNull UUID fileSrcId) {
    final VideoFileSource videoFileSource = event.getFileSource(fileSrcId);
    return videoFileSource != null
        ? videoStreamManager
            .getLocalStreamFor(fileSrcId)
            .or(() -> Optional.of(createVideoStream(videoFileSource)))
            .map(playlist -> renderPlaylist(event.getEventId(), playlist))
        : Optional.empty();
  }

  private @NotNull Collection<VideoStreamLocator> getStreamJobs(
      @NotNull VideoPlaylist videoPlaylist) {
    final UUID fileSrcId = videoPlaylist.getFileSrcId();
    final Set<Long> requestedLocatorIds = videoPlaylist.getLocatorIds().keySet();

    VideoStreamLocatorPlaylist locatorPlaylist =
        videoStreamManager
            .getLocalStreamFor(fileSrcId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException("No Video Stream for file source: " + fileSrcId));

    return locatorPlaylist.getStreamLocators().stream()
        .filter(streamLocator -> requestedLocatorIds.contains(streamLocator.getStreamLocatorId()))
        .filter(this::isNotStreaming)
        .toList();
  }

  private boolean isNotStreaming(@NotNull VideoStreamLocator locator) {
    return locator.getState().getStatus().compareTo(JobStatus.QUEUED) < 0
        && !videoStreamManager.isStreaming(locator.getStreamLocatorId());
  }

  public @NotNull VideoPlaylist renderPlaylist(
      @NotNull UUID eventId,
      @NotNull VideoStreamLocatorPlaylist playlist) {
    final UUID fileSrcId = playlist.getFileSource().getFileSrcId();
    final VideoPlaylist videoPlaylist = new VideoPlaylist(eventId, fileSrcId);

    playlist
        .getStreamLocators()
        .forEach(
            locator -> {
              final Long streamLocatorId = locator.getStreamLocatorId();
              final PartIdentifier partId = locator.getVideoFile().getTitle();
              videoPlaylist.addLocator(streamLocatorId, partId);
            });
    return videoPlaylist;
  }

  /**
   * Create playlist & asynchronously begin playlist stream
   *
   * @param videoFileSource Video source from which to create playlist
   * @return The video playlist
   */
  public @NotNull VideoStreamLocatorPlaylist createVideoStream(
      @NotNull final VideoFileSource videoFileSource) {
    return videoStreamManager.createVideoStreamFrom(videoFileSource);
  }

  public String readPlaylistFile(@NotNull Long fileId) throws Exception {
    return videoStreamManager.readPlaylistFile(fileId);
  }

  /**
   * Read video segment (.ts) data from disk
   *
   * @param partId Playlist locator ID
   * @param segmentId The filename of the requested segment (.ts extension assumed)
   * @return The video data as a Resource
   */
  public Resource getVideoSegmentResource(
      @NotNull final Long partId, @NotNull final String segmentId) {

    final Optional<VideoStreamLocator> locatorOptional = locatorService.getStreamLocator(partId);
    if (locatorOptional.isPresent()) {

      // Get path to video data
      final VideoStreamLocator streamLocator = locatorOptional.get();
      final Path storageLocation = streamLocator.getPlaylistPath().getParent();
      final String segmentFilename = String.format("%s.ts", segmentId);
      final Path segmentPath = storageLocation.resolve(segmentFilename);
      return new FileSystemResource(segmentPath);
    }
    // Resource not found
    return null;
  }

  public int getActiveStreamingTaskCount() {
    return videoStreamManager.getActiveStreamCount();
  }

  public Optional<VideoStreamLocatorPlaylist> getPlaylistForFileSource(@NotNull UUID fileSrcId) {
    return playlistService.getVideoStreamPlaylistFor(fileSrcId);
  }

  /** Destroy all currently-running video streaming tasks */
  public int killAllStreamingTasks() {
    return videoStreamManager.killAllStreams();
  }

  public int killAllStreamsFor(@NotNull final UUID fileSrcId) {
    return videoStreamManager
        .getLocalStreamFor(fileSrcId)
        .map(videoStreamManager::killAllStreamsFor)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Cannot kill streams for non-existent VideoFileSource: " + fileSrcId));
  }

  public void killStreamFor(@NotNull UUID videoFileId) {
    locatorService
        .getStreamLocatorFor(videoFileId)
        .ifPresentOrElse(
            videoStreamManager::killStreamingTask,
            () -> {
              throw new IllegalArgumentException(
                  "No VideoStreamLocator found for VideoFile: " + videoFileId);
            });
  }

  public void deleteAllVideoData(@NotNull UUID fileSrcId) throws IOException {
    Optional<VideoStreamLocatorPlaylist> playlistOptional = getPlaylistForFileSource(fileSrcId);
    if (playlistOptional.isPresent()) {
      VideoStreamLocatorPlaylist playlist = playlistOptional.get();
      deleteAllVideoData(playlist);
    } else {
      throw new IllegalArgumentException(
          "No VideoStreamLocatorPlaylist exists for VideoFileSource: " + fileSrcId);
    }
  }

  /**
   * Delete video data, including playlist & containing directory, from disk.
   *
   * @param streamPlaylist The video stream playlist for the video data
   * @throws IOException If any problems with deleting data
   */
  public void deleteAllVideoData(@NotNull final VideoStreamLocatorPlaylist streamPlaylist)
      throws IOException {
    videoStreamManager.deleteLocalStreams(streamPlaylist);
  }

  public void deleteVideoData(@NotNull UUID videoFileId) throws IOException {
    Optional<VideoStreamLocator> locatorOptional = locatorService.getStreamLocatorFor(videoFileId);
    if (locatorOptional.isPresent()) {
      VideoStreamLocator locator = locatorOptional.get();
      videoStreamManager.deleteVideoDataFromDisk(locator);
    } else {
      throw new IllegalArgumentException("No stream locator exists for VideoFile: " + videoFileId);
    }
  }

  @Transactional
  public void deleteVideoStreamPlaylist(@NotNull VideoStreamLocatorPlaylist playlist) {
    playlistService.deleteVideoStreamPlaylist(playlist);
  }

  @Transactional
  public void deleteVideoStreamLocator(@NotNull VideoStreamLocator locator) {
    locatorService.deleteStreamLocator(locator);
  }

  public VideoFileSource addOrUpdateVideoSource(
      @NotNull Collection<VideoFileSource> sources, @NotNull VideoFileSource source) {
    Optional<UUID> existing =
        sources.stream()
            .map(VideoFileSource::getFileSrcId)
            .filter(id -> id.equals(source.getFileSrcId()))
            .findAny();
    if (existing.isPresent()) {
      return videoSourceService.update(source);
    } else {
      removeIds(source);
      sources.add(source);
      return source;
    }
  }

  private void removeIds(@NotNull VideoFileSource source) {
    source.setFileSrcId(null);
    source
        .getVideoFilePacks()
        .forEach(pack -> pack.allFiles().forEach((title, file) -> file.setFileId(null)));
  }

  public Optional<VideoStreamLocator> getVideoStreamLocator(Long streamLocatorId) {
    return locatorService.getStreamLocator(streamLocatorId);
  }
}
