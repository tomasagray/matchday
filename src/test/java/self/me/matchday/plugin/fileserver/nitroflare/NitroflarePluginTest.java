/*
 * Copyright (c) 2021.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.plugin.fileserver.nitroflare;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpCookie;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.TestDataCreator;
import self.me.matchday.api.service.NetscapeCookiesService;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.util.Log;
import self.me.matchday.util.ResourceFileReader;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for Nitroflare fileserver manager")
@Disabled
class NitroflarePluginTest {

  public static final int REFRESH_HOURS = 24;
  public static final String USER_NAME = "blixblaxblox@protonmail.com";
  public static final String PASSWORD = "3wni(0wxF4qI4KQK";
  private static final String LOG_TAG = "NitroflarePluginTest";
  private static final Pattern DOWNLOAD_URL_PATTERN =
      Pattern.compile(
          "https://s\\d+\\.nitroflare\\.com/d/\\w+/20200908\\+denmark-england\\+0\\+eng\\+1080p\\.ts");
  // Test constants
  private static URL TEST_URL;
  // test resources
  private static NitroflarePlugin nitroflarePlugin;
  private static Set<HttpCookie> testCookies;

  @BeforeAll
  static void setup(
      @Autowired final NitroflarePlugin nitroflarePlugin,
      @Autowired final NetscapeCookiesService netscapeCookiesService)
      throws IOException {

    NitroflarePluginTest.nitroflarePlugin = nitroflarePlugin;

    // init test URL
    TEST_URL =
        new URL(
            "https://nitroflare.com/view/C41524E28CC3151/20200908_denmark-england_0_eng_1080p.ts");

    // Read test cookie data
    final String cookieData =
        ResourceFileReader.readTextResource(
            NitroflarePluginTest.class, "../secure_resources/nitroflare.cookies");
    assertThat(cookieData).isNotNull();
    // Parse cookie data
    NitroflarePluginTest.testCookies = netscapeCookiesService.parseNetscapeCookies(cookieData);
  }

  // === End-to-end tests ===

  @Test
  @DisplayName("Test login function")
  void login() {

    // Create user
    final FileServerUser fileServerUser = new FileServerUser(USER_NAME, PASSWORD);
    Log.i(LOG_TAG, "Attempting login with user: " + fileServerUser);

    // Attempt login
    final ClientResponse response = nitroflarePlugin.login(fileServerUser);
    if (response.statusCode().isError()) {
      response.body((inputMessage, context) -> inputMessage.getBody());
    }
    Log.i(
        LOG_TAG,
        String.format(
            "Got response: [%s] \n%s\n\nCookies:\n%s",
            response.statusCode(), response.bodyToMono(String.class), response.cookies()));

    // Perform test
    final boolean result = response.statusCode().is2xxSuccessful();
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Validate Nitroflare plugin successfully extracts download URL")
  void testCanGetDownloadUrlWithCookies() throws IOException {

    Log.i(LOG_TAG, "Attempting to get download link from URL: " + TEST_URL);
    final Optional<URL> optionalURL = nitroflarePlugin.getDownloadURL(TEST_URL, testCookies);
    assertThat(optionalURL).isPresent();

    final URL actualDownloadUrl = optionalURL.get();
    Log.i(LOG_TAG, "Found download link: " + actualDownloadUrl);

    final Matcher downloadUrlMatcher = DOWNLOAD_URL_PATTERN.matcher(actualDownloadUrl.toString());
    assertThat(downloadUrlMatcher.find()).isTrue();
  }

  // === Unit tests ===

  @Test
  @DisplayName("Test plugin accepts ONLY valid URLs")
  void acceptsUrl() throws IOException {

    // Create invalid URL
    final URL invalidUrl = new URL("https://www.yahoo.com");

    Log.i(LOG_TAG, String.format("Testing URLs: valid (%s), invalid (%s)", TEST_URL, invalidUrl));

    // Test
    assertThat(nitroflarePlugin.acceptsUrl(TEST_URL)).isTrue();
    assertThat(nitroflarePlugin.acceptsUrl(invalidUrl)).isFalse();
  }

  @Test
  @DisplayName("Test the data refresh rate is correct")
  void getRefreshRate() {

    final Duration actualRefreshRate = nitroflarePlugin.getRefreshRate();
    final Duration expectedRefreshRate = Duration.ofHours(REFRESH_HOURS);

    Log.i(
        LOG_TAG,
        String.format(
            "Testing REFRESH RATE: expected (%s), actual (%s)",
            expectedRefreshRate, actualRefreshRate));

    assertThat(actualRefreshRate).isEqualTo(expectedRefreshRate);
  }

  @Test
  @DisplayName("Verify Nitroflare plugin parses download page correctly")
  void getDownloadURL() throws IOException {

    final URL expectedDownloadUrl =
        new URL(
            "https://s99.nitroflare.com/d/66fa7c9bd6bca032b0183d3d319ae045/20201117-ESP-GER-UNL_1-1080.mkv");

    Log.i(
        LOG_TAG,
        "Attempting to read Nitroflare download link from: " + TestDataCreator.NITROFLARE_DL_URL);
    final Optional<URL> urlOptional =
        nitroflarePlugin.getDownloadURL(TestDataCreator.NITROFLARE_DL_URL, new HashSet<>());

    assertThat(urlOptional).isPresent();
    final URL actualDownloadUrl = urlOptional.get();
    Log.i(LOG_TAG, "Got download URL: " + actualDownloadUrl);
    assertThat(actualDownloadUrl).isEqualTo(expectedDownloadUrl);
  }
}
