package self.me.matchday._DEVFIXTURES;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.fileserver.FSUser;
import self.me.matchday.fileserver.IFSManager;
import self.me.matchday.util.Log;

public class DELETEMEIFSManager implements IFSManager {

  private static final String LOG_TAG = "FAKE_IFS_MANAGER";

  private FSUser user;
  private Pattern urlMatcher = Pattern.compile("https://www.inclouddrive.com/file/.*");

  private static DELETEMEIFSManager INSTANCE;
  public static DELETEMEIFSManager getInstance() {
    if(INSTANCE == null) {
      INSTANCE = new DELETEMEIFSManager();
    }
    return INSTANCE;
  }

  @Override
  public boolean login(@NotNull FSUser user) {
    this.user = user;
    return true;
  }

  @Override
  public void logout() {
    this.user = null;
  }

  @Override
  public boolean isLoggedIn() {
    return true;
  }

  @Override
  public Pattern getUrlMatcher() {
    return this.urlMatcher;
  }

  @Override
  public Optional<URL> getDownloadURL(@NotNull URL url) {
    try {
      Log.i(LOG_TAG,"Getting FAKE d/l link for input URL: " + url);

      final Pattern pattern = Pattern.compile("1-[0-9]*.[A-z]*$");
      if(pattern.matcher(url.toString()).find()) {
        Log.i(LOG_TAG, "It's THE FIRST HALF!");
        return Optional.of(new URL("http://192.168.0.101/soccer/testing/videos/1st_half.ts"));
      } else {
        Log.i(LOG_TAG, "It's the SECOND Half!");
        return Optional.of(new URL("http://192.168.0.101/soccer/testing/videos/2nd_half.ts"));
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }
}
