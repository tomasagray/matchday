package self.me.matchday;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import self.me.matchday.db.CookieRepository;
import self.me.matchday.fileserver.inclouddrive.ICDManager;
import self.me.matchday.fileserver.inclouddrive.ICDUser;
import self.me.matchday.util.Log;

@Configuration
public class LoginFileServer {

  private static final String LOG_TAG = "LoginBean";

//  @Bean
  CommandLineRunner init(CookieRepository cookieRepository) {

    return args -> {
      final ICDUser icdUser = new ICDUser("burnergm1111@gmail.com", "abithiw2itb", true);
      final ICDManager icdManager = ICDManager.getInstance();

      final URL testURL =
          new URL("https://www.inclouddrive.com/file/eR9VbKIC1mz3FxfYCxTHfg/20191201-LEI-EVE-EPL_2.ts=2nd");

      // Attempt to read page
      final Optional<URL> downloadURL = icdManager.getDownloadURL(testURL);
      if (downloadURL.isPresent()) {
        Log.i(
            LOG_TAG,
            "Already logged in; WE'RE GOOD TO GO!\nThe download URL is: " + downloadURL.get());
      } else {

        // Attempt to login
        final boolean login = icdManager.login(icdUser);
        if (login) {
          Log.i(LOG_TAG, "Successfully logged in, trying to parse D/L link...");
          final Optional<URL> loggedInDLUrl = icdManager.getDownloadURL(testURL);

          loggedInDLUrl
              .ifPresent(url -> Log.i(LOG_TAG, "Auto login + page decode WORKED!\nURL: " + url));
        } else {
          Log.e(LOG_TAG, "Auto-login failed!");
        }
      }
    };
  }
}
