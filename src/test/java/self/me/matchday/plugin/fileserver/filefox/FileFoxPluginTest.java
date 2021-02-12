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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpCookie;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.api.service.FileServerService;
import self.me.matchday.model.SecureCookie;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for FileFox fileserver manager")
@Disabled
class FileFoxPluginTest {

  private static final String LOG_TAG = "FileFoxPluginTest";

  private static FileFoxPlugin plugin;
  private static URL testDownloadLink;
  private static FileServerUser testFileServerUser;
  private static Pattern directDownloadLinkPattern;

  @BeforeAll
  static void setUp(@Autowired FileFoxPlugin plugin, @Autowired FileServerService fileServerService)
      throws MalformedURLException {

    FileFoxPluginTest.plugin = plugin;
    FileFoxPluginTest.testDownloadLink =
        new URL("https://filefox.cc/k5impa7zfhdc/20210210-EVE-TOT-FAC_1-1080.mkv");
    FileFoxPluginTest.directDownloadLinkPattern =
        Pattern.compile("^https://s\\d{2}.filefox.cc/\\w*/[\\w.-]*");

    // Get test user
    final Optional<List<FileServerUser>> usersOptional =
        fileServerService.getAllServerUsers(plugin.getPluginId());
    assertThat(usersOptional).isPresent();
    FileFoxPluginTest.testFileServerUser = usersOptional.get().get(0);
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
  @Disabled
  void login() {}

  @Test
  @DisplayName("Ensure plugin can extract direct download link")
  void getDownloadURL() throws IOException {

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
