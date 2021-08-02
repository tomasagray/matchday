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
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import self.me.matchday.api.controller.VideoStreamingController;
import self.me.matchday.api.service.EventService;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.video.M3UPlaylist;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.BaseStream;

@Service
public class VideoStreamingService {

  private static final String LOG_TAG = "VideoStreamingService";

  private final EventService eventService;
  private final VideoStreamManager videoStreamManager;
  private final VideoStreamLocatorPlaylistService playlistService;
  private final VideoStreamLocatorService videoStreamLocatorService;

  @Autowired
  public VideoStreamingService(
      final EventService eventService,
      final VideoStreamManager videoStreamManager,
      final VideoStreamLocatorPlaylistService playlistService,
      final VideoStreamLocatorService videoStreamLocatorService) {

    this.eventService = eventService;
    this.videoStreamManager = videoStreamManager;
    this.playlistService = playlistService;
    this.videoStreamLocatorService = videoStreamLocatorService;
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
   * @return An Optional containing the playlist, if one was found; empty indicates one is being
   *     created
   */
  public Optional<M3UPlaylist> getVideoStreamPlaylist(
      @NotNull final String eventId, @NotNull final String fileSrcId) {

    // todo - ensure validation happens

    final EventFileSource eventFileSource = validateFileSourceRequest(eventId, fileSrcId);
    if (eventFileSource != null) {
      final Optional<VideoStreamLocatorPlaylist> playlistOptional =
          videoStreamManager.getLocalStreamFor(fileSrcId);

      // if there is a playlist, inspect its state
      if (playlistOptional.isPresent()) {
        final VideoStreamLocatorPlaylist locatorPlaylist = playlistOptional.get();
        // determine if stream is ready
        if (videoStreamManager.isStreamReady(locatorPlaylist)) {
          // format as M3U playlist & return
          final M3UPlaylist playlist =
              getM3UPlaylistFromStreamPlaylist(eventId, fileSrcId, locatorPlaylist);
          return Optional.of(playlist);
        } else {
          // the playlist is being created, wait...
          return Optional.empty();
        }
      } else {
        // create playlist & asynchronously begin playlist stream
        final VideoStreamLocatorPlaylist locatorPlaylist =
            videoStreamManager.createVideoStreamFrom(eventFileSource);
        locatorPlaylist.getStreamLocators().forEach(videoStreamManager::beginStreaming);
        // return wait...
        return Optional.empty();
      }
    }
    // return "not found"
    return null;
  }

  /**
   * Read playlist file from disk and return as a String
   *
   * @param eventId The ID of the Event of this video data
   * @param fileSrcId The ID of the video data variant (EventFileSource)
   * @param partId Playlist locator ID
   * @return The playlist as a String
   */
  public Optional<String> readPlaylistFile(
      @NotNull final String eventId, @NotNull final String fileSrcId, @NotNull final Long partId) {

    // todo - ensure validation happens at some point!
    Log.i(
        LOG_TAG,
        String.format(
            "Attempting to read playlist file for Event: %s, File Source: %s, Locator ID: %s",
            eventId, fileSrcId, partId));

    // Get data to locate playlist file on disk
    final Optional<VideoStreamLocator> locatorOptional =
        videoStreamLocatorService.getStreamLocator(partId);
    return locatorOptional.map(this::readLocatorPlaylist);
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

    // todo - ensure validation happens!
    Log.i(
        LOG_TAG,
        String.format(
            "Reading video segment resource for Event: %s, File Source: %s, Locator ID: %s, Segment: %s",
            eventId, fileSrcId, partId, segmentId));

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

  /** Destroy all currently-running video streaming tasks */
  public int killAllStreamingTasks() {
    return videoStreamManager.killAllStreams();
  }

  public void killStreamingFor(@NotNull final String fileSrcId) {

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

  public void deleteVideoData(@NotNull final String fileSrcId) throws IOException {

    final Optional<VideoStreamLocatorPlaylist> playlistOptional =
        playlistService.getVideoStreamPlaylistFor(fileSrcId);
    if (playlistOptional.isPresent()) {
      final VideoStreamLocatorPlaylist playlist = playlistOptional.get();
      deleteVideoData(playlist);
    }
  }

  /**
   * Read playlist file; it may be concurrently being written to, so we read it reactively
   *
   * @param streamLocator The locator pointing to the required playlist file
   * @return The playlist file as a String or empty
   */
  private @Nullable String readLocatorPlaylist(@NotNull final VideoStreamLocator streamLocator) {

    final StringBuilder sb = new StringBuilder();
    final Path playlistPath = streamLocator.getPlaylistPath();
    Log.i(LOG_TAG, "Reading playlist from: " + playlistPath);

    final Flux<String> flux =
        Flux.using(() -> Files.lines(playlistPath), Flux::fromStream, BaseStream::close);
    flux.doOnNext(s -> sb.append(s).append("\n")).blockLast();
    final String result = sb.toString();
    return result.isEmpty() ? null : result;
  }

  private @NotNull M3UPlaylist getM3UPlaylistFromStreamPlaylist(
      @NotNull final String eventId,
      @NotNull final String fileSrcId,
      @NotNull final VideoStreamLocatorPlaylist locatorPlaylist) {

    // Map to M3U playlist
    final M3UPlaylist playlist = new M3UPlaylist();
    locatorPlaylist
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
   * Ensure valid data has been submitted with request
   *
   * @param eventId ID of the Event for this request
   * @param fileSrcId ID of the file source for this request
   * @return True if the event & associated file source were found, otherwise false
   */
  private @Nullable EventFileSource validateFileSourceRequest(
      @NotNull final String eventId, @NotNull final String fileSrcId) {

    // Get event from database
    final Optional<Event> eventOptional = eventService.fetchById(eventId);
    if (eventOptional.isPresent()) {
      final Event event = eventOptional.get();
      return event.getFileSource(fileSrcId);
    }
    // Request invalid
    return null;
  }
}
