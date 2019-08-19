package self.me.matchday.fileserver;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public interface IFSManager
{
    // Login to the file-server
    boolean login(FSUser user);

    // Logout of the file-server
    void logout();

    // Test whether logged in
    boolean isLoggedIn();

    // Extract download data from a given URL
    Optional<URL> getDownloadURL(URL url) throws IOException;
}
