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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import self.me.matchday.api.service.EventService;
import self.me.matchday.model.Event;
import self.me.matchday.model.video.PartIdentifier;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoPlaylist;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;

@Service
@Transactional
public class VideoStreamingService {

  @Value("${video-resources.file-read-buffer-size}")
  private int BUFFER_SIZE;

  @Value("${video-resources.file-recheck-delay-ms}")
  private int FILE_CHECK_DELAY;

  @Value("${video-resources.max-recheck-seconds}")
  private int MAX_RECHECK_TIMEOUT;

  private final EventService eventService;
  private final VideoFileSelectorService selectorService;
  private final VideoStreamManager videoStreamManager;
  private final VideoStreamLocatorPlaylistService playlistService;
  private final VideoStreamLocatorService videoStreamLocatorService;

  public VideoStreamingService(
      final EventService eventService,
      final VideoFileSelectorService selectorService,
      final VideoStreamManager videoStreamManager,
      final VideoStreamLocatorPlaylistService playlistService,
      final VideoStreamLocatorService videoStreamLocatorService) {

    this.eventService = eventService;
    this.selectorService = selectorService;
    this.videoStreamManager = videoStreamManager;
    this.playlistService = playlistService;
    this.videoStreamLocatorService = videoStreamLocatorService;
  }

  public Optional<Collection<VideoFileSource>> fetchVideoFileSources(@NotNull final UUID eventId) {

    final Optional<Event> eventOptional = eventService.fetchById(eventId);
    if (eventOptional.isPresent()) {
      final Event event = eventOptional.get();
      return Optional.of(event.getFileSources());
    }
    // Event not found
    return Optional.empty();
  }

  private void waitForFile(@NotNull Path playlistPath) {

    final File playlistFile = playlistPath.toFile();
    final Duration timeout = Duration.of(MAX_RECHECK_TIMEOUT, ChronoUnit.SECONDS);
    final Instant start = Instant.now();
    try {
      while (!playlistFile.exists()) {
        TimeUnit.MILLISECONDS.sleep(FILE_CHECK_DELAY);
        final Duration elapsed = Duration.between(start, Instant.now());
        if (elapsed.compareTo(timeout) > 0) {
          throw new InterruptedException("Timeout exceeded reading playlist file: " + playlistFile);
        }
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public Optional<VideoPlaylist> getBestVideoStreamPlaylist(@NotNull final UUID eventId) {

    final Optional<Event> eventOptional = eventService.fetchById(eventId);
    return eventOptional
        .map(
            event -> {
              // check if a stream already exists for this Event
              final Optional<VideoStreamLocatorPlaylist> playlistOptional =
                  findExistingStream(event);
              if (playlistOptional.isPresent()) {
                final VideoStreamLocatorPlaylist existingPlaylist = playlistOptional.get();
                return getVideoStreamPlaylist(
                    eventId, existingPlaylist.getFileSource().getFileSrcId());
              }
              // else...
              final VideoFileSource fileSource = selectorService.getBestFileSource(event);
              final UUID fileSrcId = fileSource.getFileSrcId();
              return getVideoStreamPlaylist(eventId, fileSrcId);
            })
        .orElse(Optional.empty());
  }

  /**
   * Determine if this Event has already been streamed, as indicated by the presence of a matching
   * VideoStreamLocatorPlaylist in the database.
   *
   * @param event The Event to query for
   * @return An Optional which will contain the playlist, or not
   */
  private @NotNull Optional<VideoStreamLocatorPlaylist> findExistingStream(@NotNull Event event) {
    return event.getFileSources().stream()
        .map(VideoFileSource::getFileSrcId)
        .map(playlistService::getVideoStreamPlaylistFor)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findAny();
  }

  /**
   * Retrieve the specified file source for the DB, via the Event
   *
   * @param eventId ID of the Event for this request
   * @param fileSrcId ID of the file source for this request
   * @return True if the event & associated file source were found, otherwise false
   */
  private @Nullable VideoFileSource getRequestedFileSource(
      @NotNull final UUID eventId, @NotNull final UUID fileSrcId) {

    // Get event from database
    final Optional<Event> eventOptional = eventService.fetchById(eventId);
    if (eventOptional.isPresent()) {
      final Event event = eventOptional.get();
      return event.getFileSource(fileSrcId);
    }
    // not found
    return null;
  }

  /**
   * Get the most recent video stream playlist for the given file source
   *
   * @param fileSrcId The ID of the VideoFileSource
   * @return An Optional containing the playlist, if one was found; empty indicates one is being
   *     created
   */
  public Optional<VideoPlaylist> getVideoStreamPlaylist(
      @NotNull final UUID eventId, @NotNull final UUID fileSrcId) {

    final VideoFileSource videoFileSource = getRequestedFileSource(eventId, fileSrcId);
    if (videoFileSource != null) {
      return videoStreamManager
          .getLocalStreamFor(fileSrcId)
          .map(playlist -> renderPlaylist(eventId, fileSrcId, playlist))
          .or(
              () -> {
                final VideoStreamLocatorPlaylist playlist = createVideoStream(videoFileSource);
                return Optional.of(renderPlaylist(eventId, fileSrcId, playlist));
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
   * Read playlist file from disk and return as a String
   *
   * @param partId Playlist locator ID
   * @return The playlist as a String
   */
  public Optional<String> readPlaylistFile(@NotNull final Long partId) {
    return videoStreamLocatorService.getStreamLocator(partId).map(this::readLocatorPlaylist);
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

    // ensure streams are started in correct order
    final List<VideoStreamLocator> locators = playlist.getStreamLocators();
    locators.sort(Comparator.comparing(VideoStreamLocator::getVideoFile));

    locators.forEach(videoStreamManager::beginStreaming);
    return playlist;
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
    // wait until playlist file actually exists
    waitForFile(playlistPath);

    final Flux<DataBuffer> fluxBuffer =
        DataBufferUtils.read(playlistPath, new DefaultDataBufferFactory(), BUFFER_SIZE);
    fluxBuffer
        .publishOn(Schedulers.boundedElastic())
        .buffer(450)
        .flatMapIterable(Function.identity())
        .doOnNext(buffer -> readBufferFromDisk(buffer, sb))
        .blockLast();
    final String result = sb.toString();
    return result.isEmpty() ? null : result;
  }

  private void readBufferFromDisk(@NotNull DataBuffer buffer, @NotNull StringBuilder sb) {
    try (final InputStream inputStream = buffer.asInputStream()) {
      final String data = new String(inputStream.readAllBytes());
      sb.append(data);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
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
