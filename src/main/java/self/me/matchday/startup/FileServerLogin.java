package self.me.matchday.startup;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.SpringVersion;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.api.service.FileServerService;
import self.me.matchday.io.TextFileReader;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.util.Log;

@Configuration
public class FileServerLogin {

  private static final String LOG_TAG = "FileServerLogin";
  private static final String LOGIN_DATA = "src/test/resources/login_data.csv";
  private static final String TEST_URL = "https://www.inclouddrive.com/file/hNWYUjpoH6kFfkZ4C3aL-A/"
      + "20110423-valencia-real-madrid-1-eng-1080p.mkv";

  //  @Bean
  CommandLineRunner initFileServer(FileServerService fileServerService) {

    Log.i(LOG_TAG, "Spring Version: " + SpringVersion.getVersion());
    Log.i(LOG_TAG, "Initialize fileserver service...");

    return args -> {

      Log.i(LOG_TAG, String.format("Determining if login is necessary; testing URL: %s", TEST_URL));
      final URL testURL = new URL(TEST_URL);
      // Attempt to translate URL with saved cookies
      final Optional<URL> downloadUrl = fileServerService.getDownloadUrl(testURL);
      if (downloadUrl.isPresent()) {
        // Good to go!
        Log.i(LOG_TAG, "Page translation successful; already logged in.");
        return;
      }

      // Otherwise, attempt login
      final boolean login = attemptLogin(fileServerService);
      if (login) {
        Log.i(LOG_TAG, "FileServer Login was SUCCESSFUL!");
      } else {
        Log.i(LOG_TAG, "****************** FileServer Login FAILED! ******************");
      }
    };
  }

  private boolean attemptLogin(@NotNull FileServerService fileServerService) throws IOException {
    // Load stored login data
    final String loginFile = TextFileReader.readLocal(Path.of(LOGIN_DATA));
    final String[] loginData = loginFile.split(",");
    // Create fileserver user
    final String username = loginData[0];
    final String password = loginData[1];
    FileServerUser fileServerUser = new FileServerUser(username, password);

    // Login to fileserver
    Log.i(LOG_TAG, String.format("Logging in with user: %s", fileServerUser));
    final ClientResponse response =
        fileServerService
            .login(fileServerUser, UUID.fromString("93161276-6c59-428d-adbe-74a5232f1647"));

    return response.statusCode().is2xxSuccessful();
  }
}
