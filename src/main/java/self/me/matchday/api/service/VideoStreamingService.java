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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.VideoStreamPlaylistLocator;
import self.me.matchday.plugin.io.diskmanager.DiskManager;
import self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import self.me.matchday.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VideoStreamingService {

  private static final String LOG_TAG = "VideoStreamingService";

  // Spring dependencies
  private final DiskManager diskManager;
  private final FFmpegPlugin ffmpegPlugin;
  private final EventService eventService;
  private final EventFileService eventFileService;
  private final PlaylistLocatorService playlistLocatorService;
  // Configuration
  @Value("${video-resources.file-storage-location}")
  private String fileStorageLocation;

  @Autowired
  public VideoStreamingService(
      final DiskManager diskManager,
      final FFmpegPlugin ffmpegPlugin,
      final EventService eventService,
      final EventFileService eventFileService,
      final PlaylistLocatorService playlistLocatorService) {

    this.diskManager = diskManager;
    this.ffmpegPlugin = ffmpegPlugin;
    this.eventService = eventService;
    this.eventFileService = eventFileService;
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
   * Read playlist file from disk and return as a String
   *
   * @param eventId The ID of the Event of this video data
   * @param fileSrcId The ID of the video data variant (EventFileSource)
   * @return The playlist as a String
   */
  public String readPlaylistFile(@NotNull final String eventId, @NotNull final String fileSrcId) {

    Log.i(
        LOG_TAG,
        String.format(
            "Attempting to read playlist file for Event: %s, File Source: %s", eventId, fileSrcId));

    // Result container
    final StringBuilder result = new StringBuilder();

    // Get data to locate playlist file on disk
    final Optional<VideoStreamPlaylistLocator> locatorOptional =
        playlistLocatorService.getPlaylistLocator(eventId, fileSrcId);
    if (locatorOptional.isPresent()) {

      // Get ref to playlist file
      final File playlistFile = locatorOptional.get().getPlaylistPath().toFile();
      if (!playlistFile.exists()) {
        Log.i(LOG_TAG, "Playlist file not found at path: " + playlistFile);
        return null;
      }
      if (!playlistFile.canRead()) {
        Log.i(LOG_TAG, "Playlist could not be opened for reading: " + playlistFile);
        return null;
      }

      // Read playlist file
      try (final BufferedReader fileReader = new BufferedReader(new FileReader(playlistFile))) {
        String line;
        while ((line = fileReader.readLine()) != null) {
          result.append(line).append("\n");
        }
      } catch (IOException e) {
        Log.e(
            LOG_TAG,
            String.format(
                "Unable to read playlist file; EventID: %s, FileSrcID: %s", eventId, fileSrcId),
            e);
      }
    } else {
      Log.d(
          LOG_TAG,
          String.format(
              "No playlist locator found for Event: %s, File Source: %s", eventId, fileSrcId));
    }

    Log.i(LOG_TAG, String.format("Read: %s bytes for playlist of event: %s", result.length(), eventId));
    return result.toString();
  }

  /**
   * Read video segment (.ts) data from disk
   *
   * @param eventId The ID of the Event for this video data
   * @param fileSrcId The ID of the video variant (EventFileSource)
   * @param segmentId The filename of the requested segment (.ts extension assumed)
   * @return The video data as a Resource
   */
  public Resource getVideoSegmentResource(
      @NotNull final String eventId,
      @NotNull final String fileSrcId,
      @NotNull final String segmentId) {

    final Optional<VideoStreamPlaylistLocator> locatorOptional =
        playlistLocatorService.getPlaylistLocator(eventId, fileSrcId);
    if (locatorOptional.isPresent()) {

      // Get path to video data
      final Path playlistPath = locatorOptional.get().getPlaylistPath();
      // Get playlist parent directory
      final Path playlistRoot = playlistPath.getParent();
      final String segmentFilename = String.format("%s.ts", segmentId);
      return new FileSystemResource(Paths.get(playlistRoot.toString(), segmentFilename));
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
   * @throws IOException If there is a problem creating video stream files
   */
  public Optional<VideoStreamPlaylistLocator> createVideoStream(
      @NotNull final String eventId, @NotNull final String fileSrcId) throws IOException {

    // Result container
    Optional<VideoStreamPlaylistLocator> result = Optional.empty();

    // Get the event from database
    final Optional<Event> eventOptional = eventService.fetchById(eventId);
    if (eventOptional.isPresent()) {

      final Event event = eventOptional.get();
      // Get the correct file source
      final EventFileSource fileSource = event.getFileSource(fileSrcId);
      // Check for adequate storage capacity
      if (fileSource != null) {
        if (diskManager.isSpaceAvailable(fileSource.getFileSize())) {

          // Refresh EventFile data (if necessary)
          eventFileService.refreshEventFileData(fileSource, false);
          // Collate URLs
          final List<URI> uris = getEventFileSrcUris(fileSource);
          // Create storage path
          final Path storageLocation =
              Files.createDirectories(
                  Paths.get(this.fileStorageLocation, event.getEventId(), fileSrcId));

          // Start FFMPEG transcoding job
          Path playlistPath = ffmpegPlugin.streamUris(uris, storageLocation).getOutputFile();
          Log.i(LOG_TAG, String.format("Created playlist file: %s", playlistPath));

          // Create playlist locator & save to DB
          final VideoStreamPlaylistLocator playlistLocator =
              playlistLocatorService.createNewPlaylistLocator(eventId, fileSrcId, playlistPath);
          // Return locator
          result = Optional.of(playlistLocator);

        } else {
          Log.i(
              LOG_TAG,
              String.format(
                  "Streaming request denied; inadequate storage capacity. "
                      + "(Requested: %s, Available: %s)",
                  fileSource.getFileSize(), diskManager.getFreeDiskSpace()));
        }
      } else {
        Log.i(
            LOG_TAG,
            String.format("File Source with ID: %s not found in Event: %s", fileSrcId, eventId));
      }
    } else {
      Log.i(LOG_TAG, "Event not found: " + eventId);
    }

    return result;
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
   * @param playlistLocator The playlist locator for the video data
   * @throws IOException If any problems with deleting data
   */
  public void deleteVideoData(@NotNull final VideoStreamPlaylistLocator playlistLocator)
      throws IOException {

    final Path playlistPath = playlistLocator.getPlaylistPath();
    // Ensure path points to a playlist file
    if (!playlistPath.endsWith("playlist.m3u8")) {
      throw new IOException(
          String.format(
              "Invalid playlist path; playlist locator: %s does not point to a playlist!",
              playlistPath));
    }

    Log.i(LOG_TAG, "Deleting video data associated with playlist locator:\n" + playlistLocator);

    // Get directory of playlist file
    final Path videoDataDir = playlistPath.getParent();
    // Delete all contents of video data directory
    Files.walkFileTree(
        videoDataDir,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
          }
        });

    // Delete playlist locator from database
    playlistLocatorService.deletePlaylistLocator(playlistLocator.getPlaylistId());
  }

  /**
   * Translate an EventFileSource into a List of URIs.
   *
   * @param eventFileSource The source of video data
   * @return A List<> of URIs pointing to video data
   */
  private List<URI> getEventFileSrcUris(@NotNull final EventFileSource eventFileSource) {

    return eventFileSource.getEventFiles().stream()
        .map(EventFile::getInternalUrl)
        .map(
            url -> {
              try {
                return url.toURI();
              } catch (URISyntaxException e) {
                Log.e(LOG_TAG, String.format("Could not parse URL -> URI: %s", url), e);
                return null;
              }
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
}
