package self.me.matchday.fileserver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Log;

public class DELETEMEIFSManager implements IFSManager {

  private static final String LOG_TAG = "FAKE_IFS_MANAGER";

  private FSUser user;


  @Override
  public void login(@NotNull FSUser user) {
    this.user = user;
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
  public Optional<URL> getDownloadURL(@NotNull URL url) {
    try {
      Log.i(LOG_TAG,"Getting FAKE d/l link for input URL: " + url);

      final Pattern pattern = Pattern.compile("1-[0-9]*.[A-z]*$");
      if(pattern.matcher(url.toString()).find()) {
        System.out.println("ITs THE FIRST HALF!");
        return Optional.of(new URL("http://192.168.0.101/soccer/testing/videos/1st_half.ts"));
      } else {
        return Optional.of(new URL("http://192.168.0.101/soccer/testing/videos/2nd_half.ts"));
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }
}
