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

package self.me.matchday.api.service;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.plugin.fileserver.FileServerPlugin;
import self.me.matchday.plugin.fileserver.TestFileServerPlugin;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for remote file server management service")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class FileServerServiceTest {

  private static final String LOG_TAG = "FileServerServiceTest";

  private static TestDataCreator testDataCreator;
  private static FileServerService fileServerService;
  private static FileServerPlugin testFileServerPlugin;
  private static FileServerUser testFileServerUser;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull final TestDataCreator testDataCreator,
      @Autowired final FileServerService fileServerService,
      @Autowired final TestFileServerPlugin testFileServerPlugin) {

    FileServerServiceTest.testDataCreator = testDataCreator;
    FileServerServiceTest.fileServerService = fileServerService;
    FileServerServiceTest.testFileServerPlugin = testFileServerPlugin;

    testFileServerUser = testDataCreator.createTestFileServerUser();
  }

  @AfterAll
  static void tearDown() {
    // delete test data
    testDataCreator.deleteFileServerUser(testFileServerUser);
  }

  // === Management ===
  @Test
  @DisplayName("Test retrieval of registered file server plugin by ID")
  void getPluginById() {

    Log.i(LOG_TAG, "Attempting to get test plugin with ID: " + testFileServerPlugin.getPluginId());
    final Optional<FileServerPlugin> pluginOptional =
        fileServerService.getPluginById(testFileServerPlugin.getPluginId());
    assertThat(pluginOptional).isPresent();

    pluginOptional.ifPresent(
        fileServerPlugin -> {
          Log.i(LOG_TAG, "Successfully found plugin: " + fileServerPlugin);
          assertThat(fileServerPlugin.getPluginId()).isEqualTo(testFileServerPlugin.getPluginId());
          assertThat(fileServerPlugin.getTitle()).isEqualTo(testFileServerPlugin.getTitle());
          assertThat(fileServerPlugin.getDescription())
              .isEqualTo(testFileServerPlugin.getDescription());
        });
  }

  @Test
  @DisplayName("Validate retrieval of all file server plugins")
  void getFileServerPlugins() {

    final List<FileServerPlugin> fileServerPlugins = fileServerService.getFileServerPlugins();

    final int expectedPluginCount = 4;
    final int actualPluginCount = fileServerPlugins.size();

    assertThat(actualPluginCount).isGreaterThanOrEqualTo(expectedPluginCount);
    assertThat(fileServerPlugins).contains(testFileServerPlugin);
  }

  @Test
  @DisplayName("Test enabling & disabling of plugin")
  void testPluginEnableAndDisable() {

    final UUID testPluginId = testFileServerPlugin.getPluginId();

    // test default enable
    Log.i(LOG_TAG, "Verifying plugin is enabled by default...");
    assertThat(fileServerService.isPluginEnabled(testPluginId)).isTrue();

    // test disable
    Log.i(LOG_TAG, "Verifying plugin can be disabled...");
    fileServerService.disablePlugin(testPluginId);
    assertThat(fileServerService.isPluginEnabled(testPluginId)).isFalse();

    // test re-enable
    Log.i(LOG_TAG, "Verifying plugin can be re-enabled...");
    fileServerService.enablePlugin(testPluginId);
    assertThat(fileServerService.isPluginEnabled(testPluginId)).isTrue();
  }

  // === Server interaction ===
  @Test
  @DisplayName("Validate login, logout & re-login functionality of file server service")
  void loginAndLogout() {

    final UUID testPluginId = testFileServerPlugin.getPluginId();

    // Login
    Log.i(LOG_TAG, "Attempting login with user: " + testFileServerUser);
    final ClientResponse actualResponse = fileServerService.login(testFileServerUser, testPluginId);

    Log.i(LOG_TAG, "Got login response: " + actualResponse.statusCode());
    // Ensure successful login
    assertThat(actualResponse.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(testFileServerUser.isLoggedIn()).isTrue();

    // Logout
    final ClientResponse logoutResponse =
        fileServerService.logout(testFileServerUser, testPluginId);
    Log.i(LOG_TAG, "Got logout response: " + logoutResponse.statusCode());
    assertThat(logoutResponse.statusCode().is2xxSuccessful()).isTrue();
    // Get fresh managed copy
    final FileServerUser userAfterLogout = getFreshManagedUser();
    assertThat(userAfterLogout.isLoggedIn()).isFalse();

    // Re-login
    fileServerService.relogin(testFileServerUser, testPluginId);
    final FileServerUser userAfterReLogin = getFreshManagedUser();
    assertThat(userAfterReLogin.isLoggedIn()).isTrue();

    // Cleanup
    fileServerService.logout(testFileServerUser, testPluginId);
  }

  @Test
  @DisplayName("Validate plugin internal URL extraction")
  void getDownloadUrl() throws IOException {

    // Ensure user is logged in
    fileServerService.login(testFileServerUser, testFileServerPlugin.getPluginId());

    final URL firstHalfUrl = testDataCreator.getFirstHalfUrl();
    assertThat(firstHalfUrl).isNotNull();

    final Optional<URL> optionalURL = fileServerService.getDownloadUrl(firstHalfUrl);
    assertThat(optionalURL.isPresent()).isTrue();

    optionalURL.ifPresent(
        url -> {
          Log.i(LOG_TAG, "Got download URL from plugin: " + url);
          assertThat(url).isNotNull();
        });
  }

  @Test
  @DisplayName("Validate plugin refresh rate retrieval in plugin service")
  void getFileServerRefreshRate() {

    final URL firstHalfUrl = testDataCreator.getFirstHalfUrl();
    assertThat(firstHalfUrl).isNotNull();

    final Duration actualServerRefreshRate =
        fileServerService.getFileServerRefreshRate(firstHalfUrl);
    final Duration expectedServerRefreshRate = testFileServerPlugin.getRefreshRate();

    assertThat(actualServerRefreshRate).isEqualTo(expectedServerRefreshRate);
  }

  // === Users ===
  @Test
  @DisplayName("Validate retrieval of all users from server")
  void getAllServerUsers() {

    final int expectedUserCount = 1;
    // Ensure user is registered with plugin
    Log.i(
        LOG_TAG,
        String.format(
            "Logging in user: %s to file server plugin: %s",
            testFileServerUser, testFileServerPlugin.getPluginId()));
    fileServerService.login(testFileServerUser, testFileServerPlugin.getPluginId());

    final List<FileServerUser> fileServerUsers =
        fileServerService.getAllServerUsers(testFileServerPlugin.getPluginId());

    Log.i(LOG_TAG, "Fetched all users from plugin: " + fileServerUsers);
    assertThat(fileServerUsers.size()).isGreaterThanOrEqualTo(expectedUserCount);
    assertThat(fileServerUsers).contains(testFileServerUser);
  }

  @Test
  @DisplayName("Validate retrieval of specific user by ID")
  void getUserById() {

    final UUID testPluginId = testFileServerPlugin.getPluginId();
    final FileServerUser testUser = testDataCreator.createTestFileServerUser();
    Log.i(
        LOG_TAG,
        String.format(
            "Logging in to File Server Plugin: %s%n with user: %s", testPluginId, testUser));
    final ClientResponse loginResponse = fileServerService.login(testUser, testPluginId);
    Log.i(LOG_TAG, "Got login response: " + loginResponse.statusCode());

    final UUID testUserId = testUser.getUserId();
    Log.i(LOG_TAG, "Attempting to retrieve user with ID: " + testUserId);
    final Optional<FileServerUser> userOptional = fileServerService.getUserById(testUserId);
    assertThat(userOptional.isPresent()).isTrue();

    userOptional.ifPresent(
        fileServerUser -> {
          Log.i(LOG_TAG, "Retrieved user from plugin: " + fileServerUser);
          assertThat(fileServerUser).isEqualTo(testUser);
        });

    fileServerService.logout(testUser, testPluginId);
  }

  @Test
  @DisplayName("Ensure file server user can be deleted from database")
  void deleteUser() {

    final UUID testUserId = testFileServerUser.getUserId();
    Log.i(LOG_TAG, "Deleting user: " + testUserId);
    fileServerService.deleteUser(testUserId);

    final Optional<FileServerUser> userOptional = fileServerService.getUserById(testUserId);
    Log.i(LOG_TAG, "User deleted; user is now: " + userOptional);
    assertThat(userOptional).isEmpty();
  }

  @NotNull
  private FileServerUser getFreshManagedUser() {

    final Optional<FileServerUser> userOptional =
        fileServerService.getUserById(testFileServerUser.getUserId());
    assertThat(userOptional).isPresent();
    return userOptional.get();
  }
}
