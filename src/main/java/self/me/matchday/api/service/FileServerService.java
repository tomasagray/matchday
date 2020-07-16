package self.me.matchday.api.service;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.plugin.fileserver.FSUser;
import self.me.matchday.plugin.fileserver.FileServerPlugin;

/**
 * Class to route requests for URL parsing (external -> internal decoding) to the appropriate File
 * Server Manager.
 */
@Service
public class FileServerService {

  private static final Duration DEFAULT_REFRESH_RATE = Duration.ofHours(4);

  private final List<FileServerPlugin> fileServerPlugins;

  @Autowired
  FileServerService(@NotNull final List<FileServerPlugin> fileServerPlugins) {
    this.fileServerPlugins = fileServerPlugins;
  }

  /**
   * Log a file server user (FSUser) into the appropriate file server.
   *
   * @param fsUser The User.
   * @param index The index of the plugin within this service's list
   * @return Was login successful? (true/false)
   */
  public boolean login(@NotNull final FSUser fsUser, final int index) {

    return
        fileServerPlugins
            .get(index)
            .login(fsUser);
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
    final FileServerPlugin pluginForUrl = getPluginForUrl(externalUrl);
    if (pluginForUrl != null) {
        // Use the FS plugin to get the internal (download) URL
        result = pluginForUrl.getDownloadURL(externalUrl);
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
    final FileServerPlugin fileServerPlugin = getPluginForUrl(url);
    // Return the recommended refresh rate for this FS manager
    return
        (fileServerPlugin != null) ?
            fileServerPlugin.getRefreshRate() :
            DEFAULT_REFRESH_RATE;
  }

  /**
   * Find the first registered file server manager which can decode the given URL.
   *
   * @param url The external URL
   * @return The first registered fileserver manager which can handle the URL.
   */
  private @Nullable FileServerPlugin getPluginForUrl(@NotNull final URL url) {

    // Determine correct FS manager
    for (FileServerPlugin fileServerPlugin : fileServerPlugins) {
      // Find first FS manager which can accept this URL
      if (fileServerPlugin.acceptsUrl(url)) {
        return fileServerPlugin;
      }
    }
    // No registered manager matches this URL
    return null;
  }
}
