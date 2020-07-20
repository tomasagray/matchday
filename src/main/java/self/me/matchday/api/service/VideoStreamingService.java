package self.me.matchday.api.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.plugin.io.diskmanager.DiskManager;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import self.me.matchday.util.Log;

@Service
public class VideoStreamingService {

  private static final String LOG_TAG = "VideoStreamingService";
  private static final String VIDEO_ROOT = "videos";

  private final DiskManager diskManager;
  private final FFmpegPlugin ffmpegPlugin;

  @Autowired
  public VideoStreamingService(@NotNull final DiskManager diskManager,
      @NotNull final FFmpegPlugin ffmpegPlugin) {

    this.diskManager = diskManager;
    this.ffmpegPlugin = ffmpegPlugin;
  }

  void streamEventFileSource(@NotNull final Event event, @NotNull final UUID fileSrcId)
      throws IOException {

    // Get the correct file source
    final EventFileSource fileSource = event.getFileSource(fileSrcId);
    if (fileSource == null) {
      Log.e(LOG_TAG,
          String.format("Event %s does not contain EventFileSource with ID: %s", event, fileSrcId));
      return;
    }

    // Check for adequate storage capacity
    if (diskManager.isSpaceAvailable(fileSource.getFileSize())) {

      // Collate URLs
      final List<URI> uris = getEventFileSrcUris(fileSource);
      // Get storage path
      final Path storageLocation =
              diskManager.createDirectories(VIDEO_ROOT, event.getEventId(), fileSrcId.toString());

      // TODO: Return playlist
      File playlist = ffmpegPlugin.streamUris(uris, storageLocation);
      Log.i(LOG_TAG, String.format("Created playlist file: %s", playlist));

    } else {
      Log.i(LOG_TAG, String.format(
          "Streaming request denied; inadequate storage capacity. (Requested: %s, Available: %s)",
          fileSource.getFileSize(), diskManager.getFreeDiskSpace()));
    }
  }

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
