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

package self.me.matchday.unit.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.api.service.FileServerUserService;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.plugin.fileserver.FileServerPlugin;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for remote file server USER management service")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class FileServerUserServiceTest {

  private static final Logger logger = LogManager.getLogger(FileServerUserServiceTest.class);

  private final FileServerUserService userService;
  private final TestDataCreator testDataCreator;
  private final FileServerPlugin testFileServerPlugin;

  private final FileServerUser testFileServerUser;

  @Autowired
  public FileServerUserServiceTest(
      @NotNull TestDataCreator testDataCreator,
      FileServerUserService userService,
      FileServerPlugin testFileServerPlugin) {
    this.userService = userService;
    this.testDataCreator = testDataCreator;
    this.testFileServerPlugin = testFileServerPlugin;
    this.testFileServerUser = testDataCreator.createTestFileServerUser();
  }

  @NotNull
  private FileServerUser getFreshManagedUser() {

    final Optional<FileServerUser> userOptional =
        userService.getUserById(testFileServerUser.getUserId());
    assertThat(userOptional).isPresent();
    return userOptional.get();
  }

  @Test
  @DisplayName("Validate login, logout & re-login functionality of file server service")
  void loginAndLogout() {

    // Login
    logger.info("Attempting login with user: " + testFileServerUser);
    final FileServerUser loggedInUser = userService.login(testFileServerUser);

    logger.info("Got logged in user: " + loggedInUser);
    assertThat(testFileServerUser.isLoggedIn()).isTrue();

    // Logout
    userService.logout(testFileServerUser.getUserId());
    // Get fresh managed copy
    final FileServerUser userAfterLogout = getFreshManagedUser();
    assertThat(userAfterLogout.isLoggedIn()).isFalse();

    // Re-login
    userService.relogin(testFileServerUser.getUserId());
    final FileServerUser userAfterReLogin = getFreshManagedUser();
    assertThat(userAfterReLogin.isLoggedIn()).isTrue();

    // Cleanup
    userService.logout(testFileServerUser.getUserId());
  }

  // === Users ===
  @Test
  @DisplayName("Validate retrieval of all users from server")
  void getAllServerUsers() {

    final int expectedUserCount = 1;
    // Ensure user is registered with plugin
    logger.info(
        "Logging in user: {} to file server plugin: {}",
        testFileServerUser,
        testFileServerPlugin.getPluginId());
    userService.login(testFileServerUser);

    final List<FileServerUser> fileServerUsers =
        userService.getAllServerUsers(testFileServerPlugin.getPluginId());

    logger.info("Fetched all users from plugin: " + fileServerUsers);
    assertThat(fileServerUsers.size()).isGreaterThanOrEqualTo(expectedUserCount);
    assertThat(fileServerUsers).contains(testFileServerUser);
  }

  @Test
  @DisplayName("Validate retrieval of specific user by ID")
  void getUserById() {

    final UUID testPluginId = testFileServerPlugin.getPluginId();
    final FileServerUser testUser = testDataCreator.createTestFileServerUser();
    logger.info("Logging in to File Server Plugin: {}%n with user: {}", testPluginId, testUser);
    final FileServerUser loggedInUser = userService.login(testUser);
    logger.info("Got logged-in user: " + loggedInUser);

    final UUID testUserId = testUser.getUserId();
    logger.info("Attempting to retrieve user with ID: " + testUserId);
    final Optional<FileServerUser> userOptional = userService.getUserById(testUserId);
    assertThat(userOptional.isPresent()).isTrue();

    userOptional.ifPresent(
        fileServerUser -> {
          logger.info("Retrieved user from plugin: " + fileServerUser);
          assertThat(fileServerUser).isEqualTo(testUser);
        });

    userService.logout(testUser.getUserId());
  }

  @Test
  @DisplayName("Ensure file server user can be deleted from database")
  void deleteUser() {

    final UUID testUserId = testFileServerUser.getUserId();
    logger.info("Deleting user: " + testUserId);
    userService.deleteUser(testUserId);

    final Optional<FileServerUser> userOptional = userService.getUserById(testUserId);
    logger.info("User deleted; user is now: " + userOptional);
    assertThat(userOptional).isEmpty();
  }
}
