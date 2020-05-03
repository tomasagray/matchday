package self.me.matchday.api.service;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.db.EventFileSrcRepository;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.VideoMetadata;
import self.me.matchday.util.Log;

@Service
@Transactional
public class EventFileService {

  private static final String LOG_TAG = "EventFileService";

  private static final int THREAD_POOL_SIZE = 12;
  // Fields
  private final ExecutorService executorService;
  private final FileServerService fileServerService;
  private final VideoMetadataService videoMetadataService;
  private final EventFileSrcRepository fileSrcRepository;

  @Autowired
  public EventFileService(FileServerService fileServerService, VideoMetadataService videoMetadataService,
      EventFileSrcRepository fileSrcRepository) {

    this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    this.fileServerService = fileServerService;
    this.videoMetadataService = videoMetadataService;
    this.fileSrcRepository = fileSrcRepository;
  }

  /**
   * Sends each EventFile from a File Source to its own thread to fetch missing or stale file data.
   * Updates the local database.
   *
   * @param eventFileSource The File Source to be refreshed.
   */
  public void refreshEventFileData(@NotNull EventFileSource eventFileSource) {

    Log.i(LOG_TAG, "Refreshing remote data for file source: " + eventFileSource);

    // TODO: Ensure the order doesn't get messed up here
    // Final result container
    final Set<EventFile> refreshedEventFiles = new LinkedHashSet<>();
    // Remote files container
    final Set<Future<EventFile>> futureEventFiles = new LinkedHashSet<>();
    // Send each link to execute in its own thread
    eventFileSource
        .getEventFiles()
        .stream()
        .filter(this::shouldRefreshData)
        .forEach(
          eventFile ->
              futureEventFiles.add(
                  executorService.submit(
                      new EventFileRefreshTask(fileServerService, videoMetadataService, eventFile)))
        );

    // Retrieve results of remote fetch operation
    futureEventFiles.forEach(
        eventFileFuture -> {
          try {
            refreshedEventFiles.add(eventFileFuture.get());
          } catch (InterruptedException | ExecutionException e) {
            Log.d(LOG_TAG, "Could not fetch remote file " + eventFileFuture, e);
          }
        });

    Log.i(LOG_TAG, "URLs successfully refreshed: " + refreshedEventFiles.size());
    // Update file source & save to DB
    eventFileSource.setEventFiles(refreshedEventFiles);
    fileSrcRepository.saveAndFlush(eventFileSource);
  }

  /**
   * Determine whether the data for the file should be refreshed.
   *
   * @param eventFile The EventFile with possibly stale data.
   * @return True/false
   */
  public boolean shouldRefreshData(@NotNull final EventFile eventFile) {

    // Last time this file's data refreshed
    final Instant lastRefresh = eventFile.getLastRefreshed().toInstant();
    final Instant now = Instant.now();
    // Time since refresh
    final Duration sinceRefresh = Duration.between(lastRefresh, now);
    // Get recommended refresh rate
    final Duration refreshRate =
        fileServerService.getFileServerRefreshRate(eventFile.getExternalUrl());
Log.d(LOG_TAG, String.format("Time since refresh: %s, Refresh rate: %s", sinceRefresh.toMillis(), refreshRate.toMillis()));
    return sinceRefresh.toMillis() > refreshRate.toMillis();
  }

  private static class EventFileRefreshTask implements Callable<EventFile> {

    private final FileServerService fileServerService;
    private final VideoMetadataService videoMetadataService;
    private final EventFile eventFile;

    public EventFileRefreshTask(FileServerService fileServerService, VideoMetadataService videoMetadataService,
        @NotNull final EventFile eventFile) {

      this.fileServerService = fileServerService;
      this.videoMetadataService = videoMetadataService;
      this.eventFile = eventFile;
    }

    /**
     * Gets missing or expired EventFile data and returns a new, complete EventFile
     *
     * @return A fully-loaded EventFile
     */
    @Override
    public EventFile call() {

      Log.i(LOG_TAG, "Refreshing data for EventFile: " + eventFile);
      try {
        final Optional<URL> downloadUrl =
            fileServerService.getDownloadUrl(eventFile.getExternalUrl());
        if (downloadUrl.isPresent()) {
          final URL url = downloadUrl.get();

          // Update remote (internal) URL
          Log.i(LOG_TAG,
              String.format("Successfully updated remote URL for EventFile: %s", eventFile));
          eventFile.setInternalUrl(url);
          // Update metadata
          Log.i(LOG_TAG, "Refreshed EventFile metadata: " + refreshEventFileMetadata());
          // Update last refresh
          eventFile.setLastRefreshed(Timestamp.from(Instant.now()));

        } else {
          throw new IOException(
              String.format("Could not parse remote URL for EventFile: %s", eventFile));
        }
      } catch (IOException e) {
        Log.e(LOG_TAG, String.format("Could not refresh remote data for EventFile: %s", eventFile),
            e);
      }
      // Return updated EventFile
      return eventFile;
    }

    /**
     * Determines whether metadata needs to be refreshed, and sets updated data if it is valid.
     *
     * @return True/false If data updated
     * @throws IOException If there is an error reading data
     */
    private boolean refreshEventFileMetadata() throws IOException {

      // Update ONLY if metadata is null
      if (eventFile.getMetadata() == null) {
        final VideoMetadata videoMetadata = videoMetadataService
            .readRemoteData(eventFile.getInternalUrl());
        // Ensure metadata successfully updated
        if (videoMetadata != null) {
          eventFile.setMetadata(videoMetadata);
          return true;
        }
      }
      // Metadata NOT updated
      return false;
    }
  }
}
