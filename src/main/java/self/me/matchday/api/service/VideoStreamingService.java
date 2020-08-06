package self.me.matchday.api.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import self.me.matchday.config.VideoResourcesConfig;
import self.me.matchday.db.VideoPlaylistLocatorRepo;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.VideoStreamPlaylistLocator;
import self.me.matchday.model.VideoStreamPlaylistLocator.VideoStreamPlaylistId;
import self.me.matchday.plugin.io.diskmanager.DiskManager;
import self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import self.me.matchday.util.Log;

@Service
public class VideoStreamingService {

  private static final String LOG_TAG = "VideoStreamingService";

  private final DiskManager diskManager;
  private final FFmpegPlugin ffmpegPlugin;
  private final EventService eventService;
  private final EventFileService eventFileService;
  private final VideoResourcesConfig videoResourcesConfig;
  private final VideoPlaylistLocatorRepo playlistLocatorRepo;

  @Autowired
  public VideoStreamingService(final DiskManager diskManager, final FFmpegPlugin ffmpegPlugin,
      final EventService eventService, final EventFileService eventFileService,
      final VideoResourcesConfig videoResourcesConfig,
      final VideoPlaylistLocatorRepo playlistLocatorRepo) {

    this.diskManager = diskManager;
    this.ffmpegPlugin = ffmpegPlugin;
    this.eventService = eventService;
    this.eventFileService = eventFileService;
    this.videoResourcesConfig = videoResourcesConfig;
    this.playlistLocatorRepo = playlistLocatorRepo;
  }

  public Optional<Collection<EventFileSource>> fetchEventFileSources(@NotNull final String eventId) {

    final Optional<Event> eventOptional = eventService.fetchById(eventId);
    if (eventOptional.isPresent()) {
      final Event event = eventOptional.get();
      return
          Optional.of(event.getFileSources());
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
  public String readPlaylistFile(@NotNull final String eventId, @NotNull final UUID fileSrcId) {

    // Result container
    final StringBuilder result = new StringBuilder();

    // Get data to locate playlist file on disk
    final Optional<VideoStreamPlaylistLocator> locatorOptional =
        playlistLocatorRepo.findById(new VideoStreamPlaylistId(eventId, fileSrcId));
    if (locatorOptional.isPresent()) {

      // Get ref to playlist file
      final File playlistFile = locatorOptional.get().getPlaylistPath().toFile();
      // Read playlist file
      try (final BufferedReader fileReader = new BufferedReader(new FileReader(playlistFile))) {
        String line;
        while ((line = fileReader.readLine()) != null) {
          result
              .append(line)
              .append("\n");
        }
      } catch (IOException e) {
        Log.e(LOG_TAG,
            String.format("Unable to read playlist file; EventID: %s, FileSrcID: %s",
                eventId, fileSrcId), e);
      }
    }
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
  public Resource getVideoSegmentResource(@NotNull final String eventId,
      @NotNull final UUID fileSrcId,
      @NotNull final String segmentId) {

    final Optional<VideoStreamPlaylistLocator> locatorOptional =
        playlistLocatorRepo.findById(new VideoStreamPlaylistId(eventId, fileSrcId));
    if (locatorOptional.isPresent()) {
      final Path playlistPath = locatorOptional.get().getPlaylistPath();
      // Get playlist parent directory
      final Path playlistRoot = playlistPath.getParent();
      final String segmentFilename = String.format("%s.ts", segmentId);
      return
          new FileSystemResource(Paths.get(playlistRoot.toString(), segmentFilename));
    }
    // Resource not found
    return null;
  }

  /**
   * Use the local installation of FFMPEG, via the FFmpeg plugin, to stream video data from a
   * remote source to local disk.
   *
   * @param eventId The ID of the Event for this video data
   * @param fileSrcId The ID of the video variant
   * @throws IOException If there is a problem creating video stream files
   */
  public void createVideoStream(@NotNull final String eventId, @NotNull final UUID fileSrcId)
      throws IOException {

    // Get the event from database
    final Optional<Event> eventOptional = eventService.fetchById(eventId);
    if (eventOptional.isPresent()) {

      final Event event = eventOptional.get();
      // Get the correct file source
      final EventFileSource fileSource = event.getFileSource(fileSrcId);
      // Check for adequate storage capacity
      if (diskManager.isSpaceAvailable(fileSource.getFileSize())) {

        // Refresh EventFile data (if necessary)
        eventFileService.refreshEventFileData(fileSource, false);
        // Collate URLs
        final List<URI> uris = getEventFileSrcUris(fileSource);
        // Create storage path
        final Path storageLocation =
            Files.createDirectories(Paths.get(
                videoResourcesConfig.getFileStorageLocation(),
                videoResourcesConfig.getVideoStorageDirname(),
                event.getEventId(), fileSrcId.toString()
            ));

        // Start FFMPEG transcoding job
        Path playlistPath = ffmpegPlugin.streamUris(uris, storageLocation);
        Log.i(LOG_TAG, String.format("Created playlist file: %s", playlistPath));

        // Create playlist locator & save to DB
        final VideoStreamPlaylistLocator playlistLocator =
            new VideoStreamPlaylistLocator(
                new VideoStreamPlaylistId(eventId, fileSrcId),
                playlistPath);
        playlistLocatorRepo.save(playlistLocator);

      } else {
        Log.i(LOG_TAG,
            String.format("Streaming request denied; inadequate storage capacity. "
                    + "(Requested: %s, Available: %s)",
            fileSource.getFileSize(), diskManager.getFreeDiskSpace()));
      }
    }
  }

  /**
   * Translate an EventFileSource into a List of URIs.
   *
   * @param eventFileSource The source of video data
   * @return A List<> of URIs pointing to video data
   */
  private List<URI> getEventFileSrcUris(@NotNull final EventFileSource eventFileSource) {

    return
        eventFileSource
            .getEventFiles()
            .stream()
            .map(EventFile::getInternalUrl)
            .map(url -> {
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
