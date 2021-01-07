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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import self.me.matchday.api.controller.VideoStreamingController;
import self.me.matchday.model.*;
import self.me.matchday.plugin.io.diskmanager.DiskManager;
import self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import self.me.matchday.util.Log;
import self.me.matchday.util.RecursiveDirectoryDeleter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.BaseStream;

@Service
public class VideoStreamingService {

  private static final String LOG_TAG = "VideoStreamingService";

  private final VideoStreamManager videoStreamManager;
  // Spring dependencies
  private final DiskManager diskManager;
  private final FFmpegPlugin ffmpegPlugin;
  private final EventService eventService;
  private final VideoStreamPlaylistService playlistService;
  private final PlaylistLocatorService playlistLocatorService;

  @Autowired
  public VideoStreamingService(
      final DiskManager diskManager,
      final FFmpegPlugin ffmpegPlugin,
      final EventService eventService,
      final VideoStreamPlaylistService playlistService,
      final VideoStreamManager videoStreamManager,
      final PlaylistLocatorService playlistLocatorService) {

    this.diskManager = diskManager;
    this.ffmpegPlugin = ffmpegPlugin;
    this.eventService = eventService;
    this.playlistService = playlistService;
    this.videoStreamManager = videoStreamManager;
    this.playlistLocatorService = playlistLocatorService;
  }

  public Optional<Collection<EventFileSource>> fetchEventFileSources(
      @NotNull final String eventId) {

    final Optional<Event> eventOptional = eventService.fetchById(eventId);
    if (eventOptional.isPresent()) {
      final Event event = eventOptional.get();
      return Optional.of(event.getFileSources());
    }
    // Event not found
    return Optional.empty();
  }

  /**
   * Get the most recent video stream playlist for the given file source
   *
   * @param fileSrcId The ID of the EventFileSource
   * @return An Optional containing the playlist, if one was found
   */
  public Optional<M3UPlaylist> getVideoStreamPlaylist(
      @NotNull final String eventId, @NotNull final String fileSrcId) {

    Log.i(LOG_TAG, String.format("Retrieving playlist for Event: %s, File Source: %s", eventId, fileSrcId));
    final Optional<VideoStreamPlaylist> playlistOptional =
        playlistService.getVideoStreamPlaylist(fileSrcId);
    return playlistOptional.map(
        playlist -> getM3UPlaylistFromStreamPlaylist(eventId, fileSrcId, playlist));
  }

  /**
   * Read playlist file from disk and return as a String
   *
   * @param eventId The ID of the Event of this video data
   * @param fileSrcId The ID of the video data variant (EventFileSource)
   * @param partId Playlist locator ID
   * @return The playlist as a String
   */
  public String readPlaylistFile(
      @NotNull final String eventId, @NotNull final String fileSrcId, @NotNull final Long partId) {

    if (!validateReadRequest(eventId, fileSrcId)) {
      Log.i(
          LOG_TAG,
          String.format(
              "Invalid request for playlist file: EventID: %s, File Source ID: %s",
              eventId, fileSrcId));
      return null;
    }

    Log.i(
        LOG_TAG,
        String.format(
            "Attempting to read playlist file for Event: %s, File Source: %s, Locator ID: %s",
            eventId, fileSrcId, partId));

    // Get data to locate playlist file on disk
    final Optional<VideoStreamLocator> locatorOptional =
        playlistLocatorService.getStreamLocator(partId);
    if (locatorOptional.isPresent()) {
      final VideoStreamLocator streamLocator = locatorOptional.get();
      // Read playlist file; it is concurrently being written to,
      // so we read it reactively
      final StringBuilder sb = new StringBuilder();
      Log.i(LOG_TAG, "Reading playlist from: " + streamLocator.getPlaylistPath());
      final Flux<String> flux =
          Flux.using(
              () -> Files.lines(streamLocator.getPlaylistPath()),
              Flux::fromStream,
              BaseStream::close);
      flux.doOnNext(s -> sb.append(s).append("\n")).blockLast();
      return sb.toString();

    } else {
      Log.d(
          LOG_TAG,
          String.format(
              "No playlist locator found for Event: %s, File Source: %s", eventId, fileSrcId));
    }
    return null;
  }

  /**
   * Read video segment (.ts) data from disk
   *
   * @param eventId The ID of the Event for this video data
   * @param fileSrcId The ID of the video variant (EventFileSource)
   * @param partId Playlist locator ID
   * @param segmentId The filename of the requested segment (.ts extension assumed)
   * @return The video data as a Resource
   */
  public Resource getVideoSegmentResource(
      @NotNull final String eventId,
      @NotNull final String fileSrcId,
      @NotNull final Long partId,
      @NotNull final String segmentId) {

    if (!validateReadRequest(eventId, fileSrcId)) {
      Log.i(
          LOG_TAG,
          String.format(
              "Invalid request submitted; Event ID: %s, File Source ID: %s", eventId, fileSrcId));
      return null;
    }

    Log.i(
        LOG_TAG,
        String.format(
            "Reading video segment resource for Event: %s, File Source: %s, Locator ID: %s, Segment: %s",
            eventId, fileSrcId, partId, segmentId));

    final Optional<VideoStreamLocator> locatorOptional =
        playlistLocatorService.getStreamLocator(partId);
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

  /**
   * Use the local installation of FFMPEG, via the FFmpeg plugin, to stream video data from a remote
   * source to local disk.
   *
   * @param eventId The ID of the Event for this video data
   * @param fileSrcId The ID of the video variant
   */
  public Optional<VideoStreamPlaylist> createVideoStream(
      @NotNull final String eventId, @NotNull final String fileSrcId) throws IOException {

    // Attempt to retrieve file source from database
    final Optional<EventFileSource> fileSourceOptional = getFileSource(eventId, fileSrcId);
    if (fileSourceOptional.isPresent() && validateStreamingRequest(fileSourceOptional.get())) {
      final EventFileSource fileSource = fileSourceOptional.get();

      // Create stream playlist
      final VideoStreamPlaylist streamPlaylist =
          playlistService.createVideoStreamPlaylist(fileSource);
      // Create streaming job for each playlist entry
      streamPlaylist.getStreamLocators().forEach(videoStreamManager::startVideoStreamTask);
      return Optional.of(streamPlaylist);
    }
    return Optional.empty();
  }

  /** Destroy all currently-running video streaming tasks */
  public void killAllStreamingTasks() {

    Log.i(
        LOG_TAG, String.format("Killing %s streaming tasks", ffmpegPlugin.getStreamingTaskCount()));
    ffmpegPlugin.interruptAllStreamTasks();
  }

  /**
   * Delete video data, including playlist & containing directory, from disk.
   *
   * @param streamPlaylist The video stream playlist for the video data
   * @throws IOException If any problems with deleting data
   */
  public void deleteVideoData(@NotNull final VideoStreamPlaylist streamPlaylist)
      throws IOException {

    // Delete data for each stream locator
    for (VideoStreamLocator streamLocator : streamPlaylist.getStreamLocators()) {

      final Path streamingPath = streamLocator.getPlaylistPath().getParent();
      Log.i(LOG_TAG, "Deleting video data associated with playlist locator:\n" + streamLocator);
      // Delete all contents of video data directory
      Files.walkFileTree(streamingPath, new RecursiveDirectoryDeleter());
    }
    // Delete data from DB
    playlistService.deleteVideoStreamPlaylist(streamPlaylist);
  }

  private @NotNull M3UPlaylist getM3UPlaylistFromStreamPlaylist(
      @NotNull final String eventId,
      @NotNull final String fileSrcId,
      @NotNull final VideoStreamPlaylist videoStreamPlaylist) {

    // Map to M3U playlist
    final M3UPlaylist playlist = new M3UPlaylist();
    videoStreamPlaylist
        .getStreamLocators()
        .forEach(
            streamLocator -> {
              try {
                // Create a segment for each playlist entry
                final EventFile eventFile = streamLocator.getEventFile();
                final String title = eventFile.getTitle().toString();
                final URI playlistUri =
                    WebMvcLinkBuilder.linkTo(
                            WebMvcLinkBuilder.methodOn(VideoStreamingController.class)
                                .getVideoPartPlaylist(
                                    eventId, fileSrcId, streamLocator.getStreamLocatorId()))
                        .toUri();
                playlist.addMediaSegment(playlistUri.toURL(), title, null);
              } catch (MalformedURLException ignore) {
              }
            });
    return playlist;
  }

  /**
   * Retrieve the requested file source from the data base
   *
   * @param eventId The ID of the Event for the file source
   * @param fileSrcId The ID of the file source
   * @return An Optional containing the file source, if found
   */
  private Optional<EventFileSource> getFileSource(
      @NotNull final String eventId, @NotNull final String fileSrcId) {

    // Result container
    Optional<EventFileSource> result = Optional.empty();

    // Get Event from database
    final Optional<Event> eventOptional = eventService.fetchById(eventId);
    if (eventOptional.isPresent()) {
      final Event event = eventOptional.get();
      // Get file source from event
      final EventFileSource fileSource = event.getFileSource(fileSrcId);
      if (fileSource != null) {
        result = Optional.of(fileSource);
      }
    }
    return result;
  }

  /**
   * Ensure file source is suitable for streaming
   *
   * @param fileSource The file source in question
   * @return True/false if the file source can reasonably be assumed to stream correctly
   */
  private boolean validateStreamingRequest(@NotNull final EventFileSource fileSource)
      throws IOException {

    // Requirements
    boolean hasEventFiles, allHaveUrls, isSpaceAvailable;

    // File source has at least one EventFile
    final List<EventFile> eventFiles = fileSource.getEventFiles();
    hasEventFiles = eventFiles.size() > 0;
    // All EventFiles have URLs
    allHaveUrls = eventFiles.stream().allMatch(eventFile -> eventFile.getExternalUrl() != null);
    // There is enough free disk space
    isSpaceAvailable = diskManager.isSpaceAvailable(fileSource.getFileSize());

    // Request must pass ALL criteria
    return hasEventFiles && allHaveUrls && isSpaceAvailable;
  }

  /**
   * Ensure valid data has been submitted with request
   *
   * @param eventId ID of the Event for this request
   * @param fileSrcId ID of the file source for this request
   * @return True if the event & associated file source were found, otherwise false
   */
  private boolean validateReadRequest(
      @NotNull final String eventId, @NotNull final String fileSrcId) {

    // Get event from database
    final Optional<Event> eventOptional = eventService.fetchById(eventId);
    if (eventOptional.isPresent()) {
      final Event event = eventOptional.get();
      final EventFileSource fileSource = event.getFileSource(fileSrcId);
      return fileSource != null;
    }
    // Request invalid
    return false;
  }

}
