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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import self.me.matchday.api.controller.VideoStreamingController;
import self.me.matchday.api.service.EventService;
import self.me.matchday.api.service.VideoFileSelectorService;
import self.me.matchday.model.Event;
import self.me.matchday.model.video.*;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.BaseStream;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Service
public class VideoStreamingService {

  private static final String LOG_TAG = "VideoStreamingService";

  private final EventService eventService;
  private final VideoFileSelectorService selectorService;
  private final VideoStreamManager videoStreamManager;
  private final StreamDelayAdviceService delayAdviceService;
  private final VideoStreamLocatorPlaylistService playlistService;
  private final VideoStreamLocatorService videoStreamLocatorService;

  @Autowired
  public VideoStreamingService(
      final EventService eventService,
      final VideoFileSelectorService selectorService,
      final VideoStreamManager videoStreamManager,
      final StreamDelayAdviceService delayAdviceService,
      final VideoStreamLocatorPlaylistService playlistService,
      final VideoStreamLocatorService videoStreamLocatorService) {

    this.eventService = eventService;
    this.selectorService = selectorService;
    this.videoStreamManager = videoStreamManager;
    this.delayAdviceService = delayAdviceService;
    this.playlistService = playlistService;
    this.videoStreamLocatorService = videoStreamLocatorService;
  }

  public Optional<Collection<VideoFileSource>> fetchVideoFileSources(
      @NotNull final String eventId) {

    final Optional<Event> eventOptional = eventService.fetchById(eventId);
    if (eventOptional.isPresent()) {
      final Event event = eventOptional.get();
      return Optional.of(event.getFileSources());
    }
    // Event not found
    return Optional.empty();
  }

  public Optional<VideoPlaylist> getBestVideoStreamPlaylist(
      @NotNull final String eventId, @NotNull final VideoPlaylistRenderer renderer) {

    final Optional<Event> eventOptional = eventService.fetchById(eventId);
    return eventOptional
        .map(
            event -> {
              final VideoFileSource fileSource = selectorService.getBestFileSource(event);
              final String fileSrcId = fileSource.getFileSrcId();
              return this.getVideoStreamPlaylist(eventId, fileSrcId, renderer);
            })
        .orElse(Optional.empty());
  }

  /**
   * Get the most recent video stream playlist for the given file source
   *
   * @param fileSrcId The ID of the VideoFileSource
   * @return An Optional containing the playlist, if one was found; empty indicates one is being
   *     created
   */
  public Optional<VideoPlaylist> getVideoStreamPlaylist(
      @NotNull final String eventId,
      @NotNull final String fileSrcId,
      @NotNull final VideoPlaylistRenderer renderer) {

    final VideoFileSource videoFileSource = getRequestedFileSource(eventId, fileSrcId);
    if (videoFileSource != null) {
      return videoStreamManager
          .getLocalStreamFor(fileSrcId)
          .map(playlist -> renderPlaylist(eventId, fileSrcId, renderer, playlist))
          .or(() -> Optional.of(createPlaylist(videoFileSource)));
    }
    // "not found"
    return Optional.empty();
  }

  /**
   * Retrieve the specified file source for the DB, via the Event
   *
   * @param eventId ID of the Event for this request
   * @param fileSrcId ID of the file source for this request
   * @return True if the event & associated file source were found, otherwise false
   */
  private @Nullable VideoFileSource getRequestedFileSource(
      @NotNull final String eventId, @NotNull final String fileSrcId) {

    // Get event from database
    final Optional<Event> eventOptional = eventService.fetchById(eventId);
    if (eventOptional.isPresent()) {
      final Event event = eventOptional.get();
      return event.getFileSource(fileSrcId);
    }
    // not found
    return null;
  }

  private @NotNull VideoPlaylist renderPlaylist(
      @NotNull String eventId,
      @NotNull String fileSrcId,
      @NotNull VideoPlaylistRenderer renderer,
      @NotNull VideoStreamLocatorPlaylist playlist) {

    long waitMillis = 0;
    String renderedPlaylist = null;

    if (delayAdviceService.isStreamReady(playlist)) {
      // Create a segment for each playlist entry
      playlist
          .getStreamLocators()
          .forEach(
              locator -> {
                final VideoFile videoFile = locator.getVideoFile();
                final Long streamLocatorId = locator.getStreamLocatorId();
                final URI playlistUri =
                    linkTo(
                            methodOn(VideoStreamingController.class)
                                .getVideoPartPlaylist(eventId, fileSrcId, streamLocatorId))
                        .toUri();
                final String title = videoFile.getTitle().toString();
                final double duration = videoFile.getDuration();
                renderer.addMediaSegment(playlistUri, title, duration);
              });
      renderedPlaylist = renderer.renderPlaylist();
    } else {
      waitMillis = delayAdviceService.getDelayAdvice(playlist);
    }
    return VideoPlaylist.builder().waitMillis(waitMillis).playlist(renderedPlaylist).build();
  }

  /**
   * Create playlist & asynchronously begin playlist stream
   *
   * @param videoFileSource Video source from which to create playlist
   * @return The video playlist
   */
  private @NotNull VideoPlaylist createPlaylist(@NotNull final VideoFileSource videoFileSource) {

    final VideoStreamLocatorPlaylist playlist =
        videoStreamManager.createVideoStreamFrom(videoFileSource);
    playlist.getStreamLocators().forEach(videoStreamManager::beginStreaming);
    final long delayAdvice = delayAdviceService.getDelayAdvice(playlist);
    return VideoPlaylist.builder().waitMillis(delayAdvice).build();
  }

  /**
   * Read playlist file from disk and return as a String
   *
   * @param eventId The ID of the Event of this video data
   * @param fileSrcId The ID of the video data variant (VideoFileSource)
   * @param partId Playlist locator ID
   * @return The playlist as a String
   */
  public Optional<String> readPlaylistFile(
      @NotNull final String eventId, @NotNull final String fileSrcId, @NotNull final Long partId) {

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

  /**
   * Read video segment (.ts) data from disk
   *
   * @param eventId The ID of the Event for this video data
   * @param fileSrcId The ID of the video variant (VideoFileSource)
   * @param partId Playlist locator ID
   * @param segmentId The filename of the requested segment (.ts extension assumed)
   * @return The video data as a Resource
   */
  public Resource getVideoSegmentResource(
      @NotNull final String eventId,
      @NotNull final String fileSrcId,
      @NotNull final Long partId,
      @NotNull final String segmentId) {

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
}
