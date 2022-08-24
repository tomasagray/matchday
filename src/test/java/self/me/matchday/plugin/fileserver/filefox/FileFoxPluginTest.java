/*
 * Copyright (c) 2022.
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
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
import self.me.matchday.api.service.FileServerUserService;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.model.SecureCookie;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for FileFox file server manager")
@Disabled
class FileFoxPluginTest {

  private static final Logger logger = LogManager.getLogger(FileFoxPluginTest.class);

  private static final String LOGIN_DATA = "src/test/secure_resources/filefox_login.csv";

  private final FileFoxPlugin plugin;
  private final FileServerUserService userService;
  private final URL testDownloadLink;
  private final Pattern directDownloadLinkPattern;

  @Autowired
  public FileFoxPluginTest(FileFoxPlugin plugin, FileServerUserService userService)
      throws IOException {

    this.plugin = plugin;
    this.userService = userService;

    this.testDownloadLink =
        new URL("https://filefox.cc/k5impa7zfhdc/20210210-EVE-TOT-FAC_1-1080.mkv");
    this.directDownloadLinkPattern = Pattern.compile("^https://s\\d{2}.filefox.cc/\\w*/[\\w.-]*");
  }

  private FileServerUser getLoggedInUser() {
    final List<FileServerUser> users = userService.getAllServerUsers(plugin.getPluginId());
    assertThat(users.size()).isGreaterThan(0);
    return users.get(0);
  }

  @Contract(" -> new")
  private @NotNull FileServerUser createTestUser() throws IOException {

    final BufferedReader reader = new BufferedReader(new FileReader(LOGIN_DATA));
    final String loginData = reader.lines().collect(Collectors.joining(" "));
    logger.info("Read login data: " + loginData);
    final List<String> strings =
        Arrays.stream(loginData.split("\""))
            .filter(s -> !s.equals(",") && !"".equals(s))
            .collect(Collectors.toList());
    assertThat(strings.size()).isEqualTo(2);
    return new FileServerUser(strings.get(0), strings.get(1), plugin.getPluginId());
  }

  @Test
  @DisplayName("Validate plugin ID")
  void getPluginId() {

    final UUID expectedPluginId = UUID.fromString("4e3389aa-3c86-4541-aaa1-9a6603753921");
    final UUID actualPluginId = plugin.getPluginId();
    logger.info("Got plugin ID: " + actualPluginId);
    assertThat(actualPluginId).isEqualTo(expectedPluginId);
  }

  @Test
  @DisplayName("Validate plugin title")
  void getTitle() {

    final String expectedTitle = "FileFox";
    final String actualTitle = plugin.getTitle();
    logger.info("Got plugin title: " + actualTitle);
    assertThat(actualTitle).isEqualTo(expectedTitle);
  }

  @Test
  @DisplayName("Validate plugin description")
  void getDescription() {

    final String expectedDescription =
        "Manager for the FileFox online file service. Translates external "
            + "links into internal, downloadable links.";
    final String actualDescription = plugin.getDescription();
    logger.info("Got plugin description:\n" + actualDescription);
    assertThat(actualDescription).isEqualTo(expectedDescription);
  }

  @Test
  @DisplayName("Validate URL acceptance")
  void acceptsUrl() {

    logger.info("Testing acceptance of URL: " + testDownloadLink);
    final boolean acceptsUrl = plugin.acceptsUrl(testDownloadLink);
    assertThat(acceptsUrl).isTrue();
  }

  @Test
  @DisplayName("Validate refresh rate")
  void getRefreshRate() {

    final Duration minRefreshRate = Duration.ofHours(1);
    final Duration maxRefreshRate = Duration.ofHours(24);
    final Duration actualRefreshRate = plugin.getRefreshRate();
    logger.info("Testing plugin refresh rate: " + actualRefreshRate);
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
    logger.info("Attempting login to FileFox file server with user: " + fileServerUser);
    final ClientResponse response = plugin.login(fileServerUser);

    // Check response
    final String responseText = response.bodyToMono(String.class).block();
    final MultiValueMap<String, ResponseCookie> cookies = response.cookies();
    final HttpStatus statusCode = response.statusCode();
    logger.info(
        "Got response: {}\nbody:\n{}\nwith cookies:\n{}", statusCode, responseText, cookies);

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

    logger.info(
        "Attempting link extraction from URL: {} with user: {}\nCookies:\n{}",
        testDownloadLink,
        testFileServerUser.getUsername(),
        cookies);

    final Optional<URL> urlOptional = plugin.getDownloadURL(testDownloadLink, cookies);
    assertThat(urlOptional).isPresent();
    urlOptional.ifPresent(
        url -> {
          logger.info("Got download link: " + url);
          final boolean urlMatches = directDownloadLinkPattern.matcher(url.toString()).find();
          assertThat(urlMatches).isTrue();
        });
  }
}
