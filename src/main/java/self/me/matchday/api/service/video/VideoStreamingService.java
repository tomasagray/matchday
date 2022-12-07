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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.model.Event;
import self.me.matchday.model.video.PartIdentifier;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoPlaylist;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;

@Service
@Transactional
public class VideoStreamingService {

  private final VideoFileSelectorService selectorService;
  private final VideoStreamManager videoStreamManager;
  private final VideoStreamLocatorPlaylistService playlistService;
  private final VideoStreamLocatorService videoStreamLocatorService;

  public VideoStreamingService(
      VideoFileSelectorService selectorService,
      VideoStreamManager videoStreamManager,
      VideoStreamLocatorPlaylistService playlistService,
      VideoStreamLocatorService videoStreamLocatorService) {

    this.selectorService = selectorService;
    this.videoStreamManager = videoStreamManager;
    this.playlistService = playlistService;
    this.videoStreamLocatorService = videoStreamLocatorService;
  }

  public Optional<VideoPlaylist> getBestVideoStreamPlaylist(@NotNull Event event) {

    // check if a stream already exists for this Event
    final Optional<VideoStreamLocatorPlaylist> playlistOptional = findExistingStream(event);
    if (playlistOptional.isPresent()) {
      final VideoStreamLocatorPlaylist existingPlaylist = playlistOptional.get();
      final UUID fileSrcId = existingPlaylist.getFileSource().getFileSrcId();
      return getOrCreateVideoStreamPlaylist(event, fileSrcId);
    }
    // else...
    final VideoFileSource fileSource = selectorService.getBestFileSource(event);
    final UUID fileSrcId = fileSource.getFileSrcId();
    return getOrCreateVideoStreamPlaylist(event, fileSrcId);
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

  /**
   * Get the most recent video stream playlist for the given file source
   *
   * @param fileSrcId The ID of the VideoFileSource
   * @return An Optional containing the playlist, if one was found; empty indicates one is being
   *     created
   */
  public Optional<VideoPlaylist> getOrCreateVideoStreamPlaylist(
      @NotNull Event event, @NotNull final UUID fileSrcId) {

    final VideoFileSource videoFileSource = event.getFileSource(fileSrcId);
    if (videoFileSource != null) {
      return videoStreamManager
          .getLocalStreamFor(fileSrcId)
          .map(playlist -> renderPlaylist(event.getEventId(), fileSrcId, playlist))
          .or(
              () -> {
                final VideoStreamLocatorPlaylist playlist = createVideoStream(videoFileSource);
                return Optional.of(renderPlaylist(event.getEventId(), fileSrcId, playlist));
              });
    }
    // "not found"
    return Optional.empty();
  }

  public @NotNull VideoPlaylist renderPlaylist(
      @NotNull UUID eventId,
      @NotNull UUID fileSrcId,
      @NotNull VideoStreamLocatorPlaylist playlist) {

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

    final VideoStreamLocatorPlaylist playlist =
        videoStreamManager.createVideoStreamFrom(videoFileSource);
    videoStreamManager.queueStreamJobs(playlist.getStreamLocators());
    return playlist;
  }

  public Optional<String> readPlaylistFile(@NotNull Long fileId) {
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

    final Optional<VideoStreamLocator> locatorOptional =
        videoStreamLocatorService.getStreamLocator(partId);
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

  /** Destroy all currently-running video streaming tasks */
  public int killAllStreamingTasks() {
    return videoStreamManager.killAllStreams();
  }

  public void killStreamingFor(@NotNull final UUID fileSrcId) {

    final Optional<VideoStreamLocatorPlaylist> playlistOptional =
        videoStreamManager.getLocalStreamFor(fileSrcId);
    if (playlistOptional.isPresent()) {
      final VideoStreamLocatorPlaylist playlist = playlistOptional.get();
      videoStreamManager.killAllStreamsFor(playlist);
    }
  }

  /**
   * Delete video data, including playlist & containing directory, from disk.
   *
   * @param streamPlaylist The video stream playlist for the video data
   * @throws IOException If any problems with deleting data
   */
  public void deleteVideoData(@NotNull final VideoStreamLocatorPlaylist streamPlaylist)
      throws IOException {
    videoStreamManager.deleteLocalStream(streamPlaylist);
  }

  public void deleteVideoData(@NotNull final UUID fileSrcId) throws IOException {

    final Optional<VideoStreamLocatorPlaylist> playlistOptional =
        playlistService.getVideoStreamPlaylistFor(fileSrcId);
    if (playlistOptional.isPresent()) {
      final VideoStreamLocatorPlaylist playlist = playlistOptional.get();
      deleteVideoData(playlist);
    }
  }
}
