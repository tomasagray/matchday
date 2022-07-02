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

package self.me.matchday.plugin.fileserver.inclouddrive;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.api.service.FileServerUserService;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.model.SecureCookie;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for InCloudDrive file server manager")
@TestMethodOrder(OrderAnnotation.class)
@Disabled
class ICDPluginTest {

  private static final Logger logger = LogManager.getLogger(ICDPluginTest.class);

  // Test constants
  private static final String FILE_LINK =
      "https://www.inclouddrive.com/file/oRMf2UsQBoXpCK5Jd17S5Q/20201128-VAL-ATM-LL_2.ts";
  private static final String URL_PATTERN =
      "https://d\\d+\\.inclouddrive.com/download.php\\?accesstoken=[\\w-]+";
  private static URL fileUrl;
  // Test resources
  private static IcdPlugin icdPlugin;
  private static FileServerUser fileServerUser;

  @BeforeAll
  static void setUp(
      @Autowired final IcdPlugin icdPlugin, @Autowired final FileServerUserService userService)
      throws IOException {

    ICDPluginTest.icdPlugin = icdPlugin;

    // Parse URL
    fileUrl = new URL(FILE_LINK);

    final List<FileServerUser> users = userService.getAllServerUsers(icdPlugin.getPluginId());
    assertThat(users.size()).isGreaterThan(0);
    fileServerUser = users.get(0);
    assertThat(fileServerUser).isNotNull();
    logger.info("Testing with user:\n" + fileServerUser);
  }

  @Test
  @Order(1)
  @DisplayName("Validate login function")
  void testLoginFunction() {

    logger.info(String.format("Testing login functionality with user: %s", fileServerUser));

    // Attempt login of user
    final ClientResponse clientResponse = icdPlugin.login(fileServerUser);

    // Extract response body
    final String responseText = clientResponse.bodyToMono(String.class).block();
    logger.info("Got response:\n" + responseText);
    logger.info("Login response: " + clientResponse.statusCode());

    assertThat(clientResponse.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseText).isNotNull().isNotEmpty();

    // inspect cookies
    clientResponse
        .cookies()
        .toSingleValueMap()
        .forEach(
            (s, responseCookie) -> {
              logger.info("Got cookie: " + responseCookie);
              assertThat(responseCookie.getName()).isNotEmpty().isNotNull();
            });
  }

  @Test
  @Order(2)
  @DisplayName("Verify properly determines that a supplied link is acceptable")
  void acceptsUrl() {

    logger.info("Testing link: {}", FILE_LINK);
    // Perform test
    assertThat(icdPlugin.acceptsUrl(fileUrl)).isTrue();
  }

  @Test
  @Order(3)
  @DisplayName("Verify can translate an input URL into a download URL")
  void testDownloadURLParsing() throws IOException {

    logger.info("Getting download link for: {}}", FILE_LINK);

    // Get user cookies to perform server interaction
    final Set<HttpCookie> cookies =
        fileServerUser.getCookies().stream()
            .map(SecureCookie::toSpringCookie)
            .collect(Collectors.toSet());
    // Ensure cookies mapped successfully
    assertThat(cookies).isNotNull().isNotEmpty();

    logger.info(
        "Attempting link extraction with user:{}\nCookies:\n{}",
        fileServerUser.getUsername(),
        cookies);

    // Get download (direct) link
    final Optional<URL> downloadURL = icdPlugin.getDownloadURL(fileUrl, cookies);
    // Primary test
    assertThat(downloadURL.isPresent()).isTrue();

    downloadURL.ifPresent(
        url -> {
          logger.info(String.format("Successfully retrieved URL: %s", url));

          // Perform URL pattern test
          final Pattern pattern = Pattern.compile(URL_PATTERN);
          final boolean matches = pattern.matcher(url.toString()).find();
          assertThat(matches).isTrue();
        });
  }
}
