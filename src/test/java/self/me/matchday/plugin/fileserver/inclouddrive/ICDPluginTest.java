package self.me.matchday.plugin.fileserver.inclouddrive;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.api.service.FileServerService;
import self.me.matchday.model.SecureCookie;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for InCloudDrive fileserver manager")
@TestMethodOrder(OrderAnnotation.class)
@Disabled
class ICDPluginTest {

  private static final String LOG_TAG = "ICDMTest";

  // Test constants
  private static final String FILE_LINK =
      "https://www.inclouddrive.com/file/oRMf2UsQBoXpCK5Jd17S5Q/20201128-VAL-ATM-LL_2.ts";
  private static URL fileUrl;
  private static final String URL_PATTERN =
      "https://d\\d+\\.inclouddrive.com/download.php\\?accesstoken=[\\w-]+";

  // Test resources
  private static IcdPlugin icdPlugin;
  private static FileServerUser fileServerUser;

  @BeforeAll
  static void setUp(
      @Autowired final IcdPlugin icdPlugin, @Autowired final FileServerService fileServerService)
      throws IOException {

    ICDPluginTest.icdPlugin = icdPlugin;

    // Parse URL
    fileUrl = new URL(FILE_LINK);

    final Optional<List<FileServerUser>> usersOptional =
        fileServerService.getAllServerUsers(icdPlugin.getPluginId());
    assertThat(usersOptional).isPresent();

    final List<FileServerUser> users = usersOptional.get();
    fileServerUser = users.get(0);

    Log.i(LOG_TAG, "Testing with user:\n" + fileServerUser);
  }

  @Test
  @Order(1)
  @DisplayName("Validate login function")
  void testLoginFunction() {

    Log.i(LOG_TAG, String.format("Testing login functionality with user: %s", fileServerUser));

    // Attempt login of user
    final ClientResponse clientResponse = icdPlugin.login(fileServerUser);

    // Extract response body
    final String responseText = clientResponse.bodyToMono(String.class).block();
    Log.i(LOG_TAG, "Got response:\n" + responseText);
    Log.i(LOG_TAG, "Login response: " + clientResponse.statusCode());

    assertThat(clientResponse.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseText).isNotNull().isNotEmpty();

    // inspect cookies
    clientResponse
        .cookies()
        .toSingleValueMap()
        .forEach(
            (s, responseCookie) -> {
              Log.i(LOG_TAG, "Got cookie: " + responseCookie);
              assertThat(responseCookie.getName()).isNotEmpty().isNotNull();
            });
  }

  @Test
  @Order(2)
  @DisplayName("Verify properly determines that a supplied link is acceptable")
  void acceptsUrl() {

    Log.i(LOG_TAG, String.format("Testing link: %s", FILE_LINK));
    // Perform test
    assertThat(icdPlugin.acceptsUrl(fileUrl)).isTrue();
  }

  @Test
  @Order(3)
  @DisplayName("Verify can translate an input URL into a download URL")
  void testDownloadURLParsing() throws IOException {

    Log.i(LOG_TAG, String.format("Getting download link for: %s", FILE_LINK));

    // Get user cookies to perform server interaction
    final List<HttpCookie> cookies =
        fileServerUser.getCookies().stream()
            .map(SecureCookie::toSpringCookie)
            .collect(Collectors.toList());
    // Ensure cookies mapped successfully
    assertThat(cookies).isNotNull().isNotEmpty();

    Log.i(
        LOG_TAG,
        String.format(
            "Attempting link extraction with user: %s\nCookies:\n%s",
            fileServerUser.getUsername(), cookies));

    // Get download (direct) link
    final Optional<URL> downloadURL = icdPlugin.getDownloadURL(fileUrl, cookies);
    // Primary test
    assertThat(downloadURL.isPresent()).isTrue();

    downloadURL.ifPresent(
        url -> {
          Log.i(LOG_TAG, String.format("Successfully retrieved URL: %s", url));

          // Perform URL pattern test
          final Pattern pattern = Pattern.compile(URL_PATTERN);
          final boolean matches = pattern.matcher(url.toString()).find();
          assertThat(matches).isTrue();
        });
  }


  // todo - Saved for later use
//  private static FileServerUser readUserDataFromDisk() throws IOException {
//
//    final String dataPath = "src/test/secure_resources/icd_test_login_data.csv";
//
//    // Read login data
//    final String loginDataString = TextFileReader.readLocal(Path.of(dataPath));
//    Log.i(LOG_TAG, String.format("Read login data from %s: %s", dataPath, loginDataString));
//    // Split login data
//    final String[] loginData = loginDataString.split(",");
//    // Create fileserver user & return
//    return new FileServerUser(loginData[0], loginData[1]);
//  }
}
