package self.me.matchday.fileserver.inclouddrive;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.fileserver.FSUser;
import self.me.matchday.io.TextFileReader;
import self.me.matchday.util.Log;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for InCloudDrive fileserver manager")
@TestMethodOrder(OrderAnnotation.class)
class ICDManagerTest {

  private static final String LOG_TAG = "ICDMTest";

  // Test fixtures
  private static final String LOGIN_DATA = "src/test/resources/login_data.csv";
  private static final String FILE_LINK =
//      "https://www.inclouddrive.com/file/MM93-UfcW4_9OsRl90N_WQ/20050412-qf-1st-chelsea-bayern"
//          + "-high-eng-1080p.mkv";
  "https://www.inclouddrive.com/file/tnm6eehVDDGmykwxwnUx5Q/19980916-md1-manutd-barcelona"
      + "-high-eng-1080p.mkv";
  private static URL FILE_URL;
  private static final String URL_PATTERN =
      "https://d\\d+\\.inclouddrive.com/download.php\\?accesstoken=[\\w-]+";

  @Autowired
  private ICDManager icdManager;
  private static FSUser fsUser;

  @BeforeAll
  static void setUp() throws IOException {

    // Read login data
    final String loginDataString = TextFileReader.readLocal(Path.of(LOGIN_DATA));
    Log.i(LOG_TAG, String.format("Read login data from %s: %s", LOGIN_DATA, loginDataString));
    // Split login data
    final String[] loginData = loginDataString.split(",");
    // Create fileserver user
    fsUser = new ICDUser(loginData[0], loginData[1]);

    // Create file URL
    FILE_URL = new URL(FILE_LINK);
  }

//  @Disabled
  @Test
  @Order(1)
  @DisplayName("Testing login function")
  void login() {

    Log.i(LOG_TAG, "Testing login functionality");
    // Attempt login of user
    final boolean login = icdManager.login(fsUser);
    Log.i(LOG_TAG, "Login success: " + login);
    assertTrue(login);
  }

  @Test
  @Order(2)
  @DisplayName("Properly determines that a supplied link is acceptable")
  void acceptsUrl() {

    Log.i(LOG_TAG, String.format("Testing link: %s", FILE_LINK));
    // Perform test
    assert icdManager.acceptsUrl(FILE_URL);
  }

  @Test
  @Order(3)
  @DisplayName("Can translate an input URL into a download URL")
  void getDownloadURL() {

    Log.i(LOG_TAG, String.format("Getting download link for: %s", FILE_LINK));

    // Get download (direct) link
    final Optional<URL> downloadURL = icdManager.getDownloadURL(FILE_URL);
    // Primary test
    assertTrue(downloadURL.isPresent());
    downloadURL.ifPresent(url -> {
      Log.i(LOG_TAG, String.format("Successfully retrieved URL: %s", url));
      // Test URL is the expected pattern
      final Pattern pattern = Pattern.compile(URL_PATTERN);
      // Perform test
      assert pattern.matcher(url.toString()).find();
    });
  }
}