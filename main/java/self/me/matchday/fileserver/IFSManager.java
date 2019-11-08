package self.me.matchday.fileserver;

import java.net.URL;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public interface IFSManager {
  // Login to the file-server
  boolean login(@NotNull FSUser user);

  // Logout of the file-server
  void logout();

  // Test whether logged in
  boolean isLoggedIn();

  // Extract download URL data from a given URL
  Optional<URL> getDownloadURL(@NotNull URL url);
}
