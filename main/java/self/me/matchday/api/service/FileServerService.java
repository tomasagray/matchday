package self.me.matchday.api.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.matchday._DEVFIXTURES.DELETEMEIFSManager;
import self.me.matchday.fileserver.FSUser;
import self.me.matchday.fileserver.IFSManager;

/**
 * Class to route requests for URL parsing (external -> internal decoding) to the appropriate File
 * Server Manager.
 */
@Service
public class FileServerService {

  private final List<IFSManager> fileServerManagers = new ArrayList<>();

  FileServerService() {
    // TODO: read FileServers from persistent storage
    final boolean b = addFSManager(DELETEMEIFSManager.getInstance());
//    fileServerManagers.add(ICDManager.getInstance());
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
  public Optional<URL> getDownloadUrl(@NotNull final URL externalUrl) {

    // Result container
    Optional<URL> result = Optional.empty();
    // Determine correct FS manager
    for (IFSManager ifsManager : fileServerManagers) {
      if(ifsManager.acceptsUrl(externalUrl)) {
        // this FS manager can handle this URL
        result = ifsManager.getDownloadURL(externalUrl);
      }
    }

    return result;
  }
}
