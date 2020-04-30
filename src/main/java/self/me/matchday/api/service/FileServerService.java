package self.me.matchday.api.service;

import java.awt.image.DataBufferUShort;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.fileserver.FSUser;
import self.me.matchday.fileserver.IFSManager;
import self.me.matchday.fileserver.inclouddrive.ICDManager;
import self.me.matchday.util.Log;

/**
 * Class to route requests for URL parsing (external -> internal decoding) to the appropriate File
 * Server Manager.
 */
@Service
public class FileServerService {

  private static final String LOG_TAG = "FileServerService";
  private static final Duration DEFAULT_REFRESH_RATE = Duration.ofHours(4);

  private final List<IFSManager> fileServerManagers = new ArrayList<>();

  @Autowired
  FileServerService(ICDManager icdManager) {

    // TODO: read FileServers from persistent storage
    Log.i(LOG_TAG, "Adding InCloudDrive FS manager: " + addFSManager(icdManager));
//    final boolean b = addFSManager(DELETEMEIFSManager.getInstance());
  }

  public boolean addFSManager(@NotNull final IFSManager ifsManager) {
    return this.fileServerManagers.add(ifsManager);
  }

  /**
   * Log a file server user (FSUser) into the appropriate file server.
   * @param fsUser The User.
   * @return Was login successful? (true/false)
   */
  public boolean login(@NotNull final FSUser fsUser) {
    // TODO: Implement login of user to correct file server!
    return fileServerManagers.get(0).login(fsUser);
  }

  /**
   * Wraps the getDownloadUrl() method of each File Server, and routes the request to the appropriate
   * one.
   * @param externalUrl The external-facing (public) URL
   * @return The internal (private) URL.
   */
  public Optional<URL> getDownloadUrl(@NotNull final URL externalUrl) throws IOException {

    // Result container
    Optional<URL> result = Optional.empty();
    // Get correct FS manager
    final IFSManager managerForUrl = getManagerForUrl(externalUrl);
    if (managerForUrl != null) {
        // Use the FS manager to get the internal (download) URL
        result = managerForUrl.getDownloadURL(externalUrl);
    }
    return result;
  }

  /**
   * Returns the recommended refresh rate for data associated with the given URL. If this cannot be
   * determined from the URL, returns a default value instead.
   *
   * @param url The external URL for the fileserver
   * @return The recommended refresh rate.
   */
  public Duration getFileServerRefreshRate(@NotNull final URL url) {

    // Get the fileserver manager for this URL
    final IFSManager ifsManager = getManagerForUrl(url);
    // Return the recommended refresh rate for this FS manager
    return (ifsManager != null) ? ifsManager.getRefreshRate() : DEFAULT_REFRESH_RATE;
  }

  /**
   * Find the first registered file server manager which can decode the given URL.
   *
   * @param url The external URL
   * @return The first registered fileserver manager which can handle the URL.
   */
  private @Nullable IFSManager getManagerForUrl(@NotNull final URL url) {

    // Determine correct FS manager
    for (IFSManager ifsManager: fileServerManagers) {
      // Find first FS manager which can accept this URL
      if (ifsManager.acceptsUrl(url)) {
        return ifsManager;
      }
    }
    // No registered manager matches this URL
    return null;
  }

}
