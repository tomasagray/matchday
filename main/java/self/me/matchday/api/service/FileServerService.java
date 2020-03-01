package self.me.matchday.api.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.matchday._DEVFIXTURES.DELETEMEIFSManager;
import self.me.matchday.fileserver.IFSManager;

/**
 * Class to route requests for URL parsing (external -> internal decoding) to the appropriate File
 * Server Manager.
 */
@Service
public class FileServerService {

  private final List<IFSManager> fileServerManagers;

  FileServerService() {
    fileServerManagers = new ArrayList<>();
    // todo: move this elsewhere, so managers can be added at runtime, change to REAL ICD manager
    fileServerManagers.add(DELETEMEIFSManager.getInstance());
//    fileServerManagers.add(ICDManager.getInstance());
  }

  public boolean addFSManager(@NotNull final IFSManager ifsManager) {
    return this.fileServerManagers.add(ifsManager);
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
      final Matcher matcher = ifsManager.getUrlMatcher().matcher(externalUrl.toString());
      if (matcher.find()) {
        // this FS manager can handle this URL
        result = ifsManager.getDownloadURL(externalUrl);
      }
    }

    return result;
  }
}
