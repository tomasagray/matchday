package self.me.matchday.api.service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
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
    // Determine correct FS manager
    for (IFSManager ifsManager : fileServerManagers) {
      if(ifsManager.acceptsUrl(externalUrl)) {
        // this FS manager can handle this URL
        result = ifsManager.getDownloadURL(externalUrl);
        // quit searching
        break;
      }
    }
    return result;
  }
}
