/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.fileserver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import self.me.matchday.TestConstants;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.feed.blogger.Blogger;
import self.me.matchday.feed.blogger.galataman.GalatamanBlog;
import self.me.matchday.feed.blogger.galataman.GalatamanPost;
import self.me.matchday.feed.blogger.galataman.GalatamanPostProcessor;
import self.me.matchday.fileserver.inclouddrive.ICDManager;
import self.me.matchday.fileserver.inclouddrive.ICDUser;
import self.me.matchday.io.TextFileReader;
import self.me.matchday.util.Log;

// todo: re-enable test
@Disabled
@TestMethodOrder(OrderAnnotation.class)
class ICDManagerTest {
  private static final String LOG_TAG = "ICDManagerTest";

  // Test parameters
  private static final int URL_LIMIT = 2;
  private static final String LINK_PATTERN =
      "https://d\\d{2}.inclouddrive.com/download.php\\?accesstoken=([A-z,0-9-])*";
  private static final String testURL =
      TestConstants.REMOTE_KNOWN_GOOD_JSON;
      //"http://galatamanhdf.blogspot.com/feeds/posts/default?alt=json";

  // Test resources
  private static final List<URL> urls = new ArrayList<>();
  private static final ICDManager icdm = ICDManager.getInstance();
  private static FSUser user;
  private static Blogger blog;

  // Setup
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
                gp
                    .getEventFileSources().stream()
                    .map(EventFileSource::getEventFiles)
                    .flatMap(Collection::stream)
                    .map(EventFile::getExternalUrl)
                    .forEach(urls::add);
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
    String loginFile = "C:\\Users\\Tomas\\Code\\Source\\IdeaProjects\\Matchday\\src\\test\\resources\\self\\me\\matchday\\fileserver\\login_data.csv";
    final String loginData = TextFileReader.readLocal(Path.of(loginFile));
    Log.d(LOG_TAG, "Loaded login data: " + loginData);

    // Explode data
    String[] data = loginData.split(",");
    // Assign loaded data
    user = new ICDUser(data[0]);
    user.setPassword(data[1]);
    if (data[2].toLowerCase().equals("true")) user.setKeepLoggedIn(true);
  }

  // Tests
  @Test
  @Order(1)
  @DisplayName("Login test; ensure login functionality")
  void login() {
    Log.i(LOG_TAG, "Data:\n" + user.toString());
    // Attempt to login
    icdm.login(user);

    // Print results
    Log.i(LOG_TAG, "Login Successful?:\n" + icdm.isLoggedIn() + "\n");

    // Run the tests
    assertTrue(icdm.isLoggedIn());
  }

  @Disabled
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
        blog.getEntries().get(0)
            .getEventFileSources().stream()
            .map(EventFileSource::getEventFiles)
            .flatMap(Collection::stream)
            .map(EventFile::getExternalUrl)
            .collect(Collectors.toList());
    URL testUrl = urls.get(0);

    Log.d(LOG_TAG, "Testing URL: " + testUrl);
    // Attempt to extract D/L URL; should fail
    Optional<URL> downloadURL = icdm.getDownloadURL(urls.get(0));
    // Perform test
    assertFalse(downloadURL.isPresent());

    Log.d(LOG_TAG, "Test passed; we could NOT read D/L URL after logging out.");
  }

//  @Disabled
  @Tag("ICD")
  @Order(2)
  @DisplayName("Test the getDownloadURL method can get the D/L link")
  @ParameterizedTest(name = " for: {index}: {0}")
  @MethodSource("getUrls")
  void getDownloadURL(URL url) {
    // Ensure logged in
    if (!icdm.isLoggedIn()) {
      Log.i(LOG_TAG, "User not logged in; logging in now...");
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
    // Ensure the link is the one we are expecting
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

