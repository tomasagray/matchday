package self.me.matchday.fileserver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import self.me.matchday.feed.Blogger;
import self.me.matchday.feed.IMatchFileSource;
import self.me.matchday.feed.galataman.GalatamanBlog;
import self.me.matchday.feed.galataman.GalatamanPost;
import self.me.matchday.feed.galataman.GalatamanPostProcessor;
import self.me.matchday.util.Log;

@TestMethodOrder(OrderAnnotation.class)
class ICDManagerTest {
  private static final String LOG_TAG = "ICDManagerTest";

  // Test parameters
  // -----------------------------------------------------------------------------------
  private static final int URL_LIMIT = 3;
  private static final String LINK_PATTERN =
      "https://d\\d{2}.inclouddrive.com/download.php\\?accesstoken=([A-z,0-9-])*";
  private static final String testURL =
      "http://galatamanhdf.blogspot.com/feeds/posts/default?alt=json";

  // Test resources
  private static final List<URL> urls = new ArrayList<>();
  private static final ICDManager icdm = ICDManager.getInstance();
  private static FSUser user;
  private static Blogger blog;

  // Setup
  // ----------------------------------------------------------------------------------
  @BeforeAll
  static void setup() throws IOException {
    // Setup user
    loadUserData();

    // Setup blog
    blog = new GalatamanBlog(new URL(testURL), new GalatamanPostProcessor());

    // Populate url list with all available URLs,
    // regardless of match/post
    blog.getEntries()
        .forEach(
            gp -> {
              if (gp instanceof GalatamanPost) {
                ((GalatamanPost) gp)
                    .getMatchFileSources().stream()
                        .map(IMatchFileSource::getUrls)
                        .forEach(urls::addAll);
              }
            });

    Log.i(LOG_TAG, "Successfully created: " + blog.toString());
    Log.i(LOG_TAG, "Found: " + urls.size() + " URLs");
  }

  /**
   * Provider method for tests
   *
   * @return A Stream of Arguments (of URLs)
   */
  private static Stream<Arguments> getUrls() {
    return urls.stream().limit(URL_LIMIT).map(Arguments::of);
  }

  /**
   * Loads user login data from a CSV file. Expects: <Username>,<password>,<keepLoggedIn>
   *
   * @throws IOException If cannot read file.
   */
  private static void loadUserData() throws IOException {
    // Read user data from file
    String loginData =
        IOUtils.toString(
            ICDManagerTest.class.getResourceAsStream("login_data.csv"),
            StandardCharsets.UTF_8.toString());
    Log.d(LOG_TAG, "Loaded login data: " + loginData);

    // Explode data
    String[] data = loginData.split(",");
    // Assign loaded data
    user = new ICDUser(data[0]);
    user.setPassword(data[1]);
    if (data[2].toLowerCase().equals("true")) user.setKeepLoggedIn(true);
  }

  // Tests
  // ----------------------------------------------------------------------------------
  @Test
  @Order(1)
  @DisplayName("Login test; ensure login functionality")
  void login() {
    Log.i(LOG_TAG, "POSTing to URL: " + ICDManager.ICDData.getLoginURL());
    Log.i(LOG_TAG, "Data:\n" + user.toString());
    // Attempt to login
    icdm.login(user);

    // Print results
    Log.i(LOG_TAG, "Response:\n" + icdm.getLoginResponse());
    Log.i(LOG_TAG, "Login Successful?:\n" + icdm.isLoggedIn() + "\n");

    // Run the tests
    assertTrue(icdm.isLoggedIn());
  }

  @Test
  @Order(3)
  @DisplayName("Logout disables page read; make sure we CAN'T read download page")
  void logoutTest() {
    // Perform logout
    icdm.logout();

    // TESTS: ************************
    assertFalse(icdm.isLoggedIn());
    Log.d(LOG_TAG, "ICDM successfully logged out.");

    // Get a sample URL from the MatchSource
    List<URL> urls =
        ((GalatamanPost) blog.getEntries().get(0))
            .getMatchFileSources().stream()
                .map(IMatchFileSource::getUrls)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    URL testUrl = urls.get(0);

    Log.d(LOG_TAG, "Testing URL: " + testUrl);
    // Attempt to extract D/L URL; should fail
    Optional<URL> downloadURL = icdm.getDownloadURL(urls.get(0));
    // Perform test
    assertFalse(downloadURL.isPresent());

    Log.d(LOG_TAG, "Test passed; we could NOT read D/L URL after logging out.");
  }

  @Tag("ICD")
  //    @Disabled
  @Order(2)
  @DisplayName("Test the getDownloadURL method can get the D/L link")
  @ParameterizedTest(name = " for: {index}: {0}")
  @MethodSource("getUrls")
  void getDownloadURL(URL url) {
    // Ensure logged in
    if (!icdm.isLoggedIn()) {
      Log.i(LOG_TAG, "User was not logged in; logging in now...");
      icdm.login(user);
    }

    Log.i(LOG_TAG, "Testing url: " + url.toString());
    Optional<URL> op_url = icdm.getDownloadURL(url);

    // TEST: *********************************
    // We DID find the link, right?
    assertTrue(op_url.isPresent());
    String theLink = op_url.get().toString();
    Log.i(LOG_TAG, "\t|_Found link: " + theLink);

    // TEST: *********************************
    // Ensure link is the one we are expecting
    assertTrue(theLink.matches(LINK_PATTERN));
  }

  @Disabled
  @Test
  @Order(5)
  @DisplayName("Ensure we can properly save all cookie data")
  void saveCookieData() {}

  @Disabled
  @Test
  @Order(6)
  @DisplayName("Ensure we can retrieve all needed cookie data from local store")
  void loadCookieData() {}
}
