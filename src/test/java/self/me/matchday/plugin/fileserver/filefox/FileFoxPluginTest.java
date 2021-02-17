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

package self.me.matchday.plugin.fileserver.filefox;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.api.service.FileServerService;
import self.me.matchday.model.SecureCookie;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for FileFox fileserver manager")
class FileFoxPluginTest {

  private static final String LOG_TAG = "FileFoxPluginTest";

  private static final String LOGIN_DATA = "src/test/secure_resources/filefox_login.csv";

  private static FileFoxPlugin plugin;
  private static FileServerService fileServerService;
  private static URL testDownloadLink;
  private static Pattern directDownloadLinkPattern;

  @BeforeAll
  static void setUp(@Autowired FileFoxPlugin plugin, @Autowired FileServerService fileServerService)
      throws IOException {

    FileFoxPluginTest.plugin = plugin;
    FileFoxPluginTest.fileServerService = fileServerService;

    FileFoxPluginTest.testDownloadLink =
        new URL("https://filefox.cc/k5impa7zfhdc/20210210-EVE-TOT-FAC_1-1080.mkv");
    FileFoxPluginTest.directDownloadLinkPattern =
        Pattern.compile("^https://s\\d{2}.filefox.cc/\\w*/[\\w.-]*");
  }

  private static FileServerUser getLoggedInUser() {

    final Optional<List<FileServerUser>> usersOptional =
        fileServerService.getAllServerUsers(plugin.getPluginId());
    assertThat(usersOptional).isPresent();
    return usersOptional.get().get(0);
  }

  private static FileServerUser createTestUser() throws IOException {

    final BufferedReader reader = new BufferedReader(new FileReader(LOGIN_DATA));
    final String loginData = reader.lines().collect(Collectors.joining(" "));
    Log.i(LOG_TAG, "Read login data: " + loginData);
    final List<String> strings =
        Arrays.stream(loginData.split("\""))
            .filter(s -> !s.equals(",") && !"".equals(s))
            .collect(Collectors.toList());
    assertThat(strings.size()).isEqualTo(2);
    return new FileServerUser(strings.get(0), strings.get(1));
  }

  @Test
  @DisplayName("Validate plugin ID")
  void getPluginId() {

    final UUID expectedPluginId = UUID.fromString("4e3389aa-3c86-4541-aaa1-9a6603753921");
    final UUID actualPluginId = plugin.getPluginId();
    Log.i(LOG_TAG, "Got plugin ID: " + actualPluginId);
    assertThat(actualPluginId).isEqualTo(expectedPluginId);
  }

  @Test
  @DisplayName("Validate plugin title")
  void getTitle() {

    final String expectedTitle = "FileFox";
    final String actualTitle = plugin.getTitle();
    Log.i(LOG_TAG, "Got plugin title: " + actualTitle);
    assertThat(actualTitle).isEqualTo(expectedTitle);
  }

  @Test
  @DisplayName("Validate plugin description")
  void getDescription() {

    final String expectedDescription =
        "Manager for the FileFox online file service. Translates external "
            + "links into internal, downloadable links.";
    final String actualDescription = plugin.getDescription();
    Log.i(LOG_TAG, "Got plugin description:\n" + actualDescription);
    assertThat(actualDescription).isEqualTo(expectedDescription);
  }

  @Test
  @DisplayName("Validate URL acceptance")
  void acceptsUrl() {

    Log.i(LOG_TAG, "Testing acceptance of URL: " + testDownloadLink);
    final boolean acceptsUrl = plugin.acceptsUrl(testDownloadLink);
    assertThat(acceptsUrl).isTrue();
  }

  @Test
  @DisplayName("Validate refresh rate")
  void getRefreshRate() {

    final Duration minRefreshRate = Duration.ofHours(1);
    final Duration maxRefreshRate = Duration.ofHours(24);
    final Duration actualRefreshRate = plugin.getRefreshRate();
    Log.i(LOG_TAG, "Testing plugin refresh rate: " + actualRefreshRate);
    assertThat(actualRefreshRate)
        .isGreaterThanOrEqualTo(minRefreshRate)
        .isLessThanOrEqualTo(maxRefreshRate);
  }

  @Test
  @DisplayName("Test login capability")
  void login() throws IOException {

    // Read test data
    final FileServerUser fileServerUser = createTestUser();
    // Perform login
    Log.i(LOG_TAG, "Attempting login to FileFox file server with user: " + fileServerUser);
    final ClientResponse response = plugin.login(fileServerUser);

    // Check response
    final String responseText = response.bodyToMono(String.class).block();
    final MultiValueMap<String, ResponseCookie> cookies = response.cookies();
    final HttpStatus statusCode = response.statusCode();
    Log.i(
        LOG_TAG,
        String.format(
            "Got response: %s\nbody:\n%s\nwith cookies:\n%s", statusCode, responseText, cookies));

    final boolean loginSuccessful = statusCode.is2xxSuccessful();
    assertThat(loginSuccessful).isTrue();
  }

  @Test
  @DisplayName("Ensure plugin can extract direct download link")
  void getDownloadURL() throws IOException {

    final FileServerUser testFileServerUser = getLoggedInUser();

    final Set<HttpCookie> cookies =
        testFileServerUser.getCookies().stream()
            .map(SecureCookie::toSpringCookie)
            .collect(Collectors.toSet());

    Log.i(
        LOG_TAG,
        String.format(
            "Attempting link extraction from URL: %s with user: %s\nCookies:\n%s",
            testDownloadLink, testFileServerUser.getUsername(), cookies));

    final Optional<URL> urlOptional = plugin.getDownloadURL(testDownloadLink, cookies);
    assertThat(urlOptional).isPresent();
    urlOptional.ifPresent(
        url -> {
          Log.i(LOG_TAG, "Got download link: " + url);
          final boolean urlMatches = directDownloadLinkPattern.matcher(url.toString()).find();
          assertThat(urlMatches).isTrue();
        });
  }
}
