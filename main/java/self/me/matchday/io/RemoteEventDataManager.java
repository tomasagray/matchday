package self.me.matchday.io;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.EventFileSource;
import self.me.matchday.fileserver.DELETEMEIFSManager;
import self.me.matchday.fileserver.IFSManager;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFile.EventFileSorter;
import self.me.matchday.model.EventFile.EventPartIdentifier;
import self.me.matchday.util.Log;

public class RemoteEventDataManager {

  private static final String LOG_TAG = "RemoteEventDataManager";

  // Singleton
  private static RemoteEventDataManager INSTANCE;

  public static RemoteEventDataManager getInstance() {
    if (INSTANCE == null) {
      // TODO: Change IFS implementation
      INSTANCE =
          new RemoteEventDataManager(Executors.newCachedThreadPool(), new DELETEMEIFSManager());
    }

    return INSTANCE;
  }

  // Fields
  private final ExecutorService executorService;
  private final IFSManager ifsManager;

  private RemoteEventDataManager(
      @NotNull ExecutorService executorService, @NotNull IFSManager ifsManager) {
    this.executorService = executorService;
    this.ifsManager = ifsManager;
  }

  @NotNull
  public List<EventFile> getEventFiles(@NotNull EventFileSource eventFileSource) {

    // TODO: Ensure this function works as expected. Are threads doubling up?
    // Final result container
    final List<EventFile> eventFiles = new ArrayList<>();
    // Remote files container
    final List<Future<EventFile>> futureEventFiles = new ArrayList<>();
    // Remote links for this file source
    final Map<URL, EventPartIdentifier> urls = eventFileSource.getUrls();

    // Send each link to execute in its own thread
    urls.forEach(
        (url, title) ->
            futureEventFiles.add(
                executorService.submit(new EventFileFetchTask(ifsManager, url, title))));

    // Retrieve results of remote fetch operation
    futureEventFiles.forEach(
        eventFileFuture -> {
          try {
            eventFiles.add(eventFileFuture.get());
          } catch (InterruptedException | ExecutionException e) {
            Log.d(LOG_TAG, "Could not fetch remote file " + eventFileFuture.toString());
          }
        });

    // Sort results
    eventFiles.sort(new EventFileSorter());
    return eventFiles;
  }

  private static class EventFileFetchTask implements Callable<EventFile> {

    private final IFSManager ifsManager;
    private final URL externalUrl;
    private URL downloadUrl;
    private final EventPartIdentifier title;
    // TODO: address duration - how to fetch quickly vs. how necessary?
    private float duration = 3047.701745f;

    public EventFileFetchTask(
        @NotNull final IFSManager ifsManager,
        @NotNull URL url,
        @NotNull EventPartIdentifier title) {
      this.ifsManager = ifsManager;
      this.externalUrl = url;
      this.title = title;
    }

    @Override
    public EventFile call() {
      // Get the remote URL
      ifsManager.getDownloadURL(externalUrl).ifPresent(url -> this.downloadUrl = url);
      // Get remote metadata
      getFileMetadata();
      return new EventFile(this.title, this.downloadUrl, this.duration);
    }

    /** Read video file metadata from remote source using FFMPEG. */
    private void getFileMetadata() {
      // TODO: Is this even necessary? Rewrite ffprobe myself?
      try {
        final FFprobe fFprobe = new FFprobe("C:\\Program Files\\ffmpeg\\bin\\ffprobe.exe");
        final FFmpegProbeResult probeResult = fFprobe.probe(downloadUrl.toString());
        this.duration = (float) probeResult.getFormat().duration;
        System.out.println("The duration is: " + probeResult.getFormat().duration);

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
