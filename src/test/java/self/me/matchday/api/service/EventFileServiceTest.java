/*
 * Copyright (c) 2020.
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
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.db.FileServerUserRepo;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.SecureCookie;
import self.me.matchday.plugin.fileserver.FileServerPlugin;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.plugin.io.ffmpeg.FFmpegMetadata;
import self.me.matchday.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static self.me.matchday.model.EventFileSource.Resolution.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for EventFile refresh service")
class EventFileServiceTest {

  private static final String LOG_TAG = "EventFileServiceTest";

  // Test resources
  private static EventFileSource testEventFileSrc;
  private static EventFileService eventFileService;
  private static FileServerUserRepo userRepo;
  private static FileServerUser testUser;

  private static class TestEventFileServicePlugin implements FileServerPlugin {

    private static final Pattern urlPattern =
        Pattern.compile("http[s]?://192.168.0.101/stream2stream/[\\w-/.]*");

    private final UUID pluginId;

    TestEventFileServicePlugin() {
      pluginId = UUID.randomUUID();
    }

    @Override
    public @NotNull ClientResponse login(@NotNull FileServerUser user) {
      return ClientResponse.create(HttpStatus.OK).build();
    }

    @Override
    public boolean acceptsUrl(@NotNull URL url) {
      return urlPattern.matcher(url.toString()).find();
    }

    @Override
    public @NotNull Duration getRefreshRate() {
      return Duration.ofDays(1_000L);
    }

    @Override
    public Optional<URL> getDownloadURL(@NotNull URL url, @NotNull Collection<HttpCookie> cookies) {
      return
              Optional.of(url);
    }

    @Override
    public UUID getPluginId() {
      return pluginId;
    }

    @Override
    public String getTitle() {
      return "Test file server plugin";
    }

    @Override
    public String getDescription() {
      return null;
    }
  }

  @BeforeAll
  static void setUp(
          @Autowired final EventFileService eventFileService,
          @Autowired final FileServerService fileServerService,
          @Autowired final FileServerUserRepo userRepo)
      throws MalformedURLException {

    EventFileServiceTest.eventFileService = eventFileService;
    EventFileServiceTest.userRepo = userRepo;

    // Create & register test file server plugin
    final TestEventFileServicePlugin testFileServerPlugin = new TestEventFileServicePlugin();
    fileServerService.getFileServerPlugins().add(testFileServerPlugin);

    // Login test user to file server plugin
    testUser = new FileServerUser("test", "test");
    testUser.loginToServer(
            testFileServerPlugin.getPluginId().toString(),
            List.of(new SecureCookie("test name", "test val")));
    userRepo.save(testUser);

    // Create test EventFileSource
    testEventFileSrc = createTestEventFileSrc();
  }

  private static EventFileSource createTestEventFileSrc() throws MalformedURLException {

    // Create test EventFiles
    final EventFile firstHalf =
        new EventFile(
            EventFile.EventPartIdentifier.FIRST_HALF,
            new URL("http://192.168.0.101/stream2stream/barca-rm-2009/1.ts"));
    // normally done by Spring
    firstHalf.setEventFileId(Long.MAX_VALUE - 1);

    final EventFile secondHalf =
        new EventFile(
            EventFile.EventPartIdentifier.SECOND_HALF,
            new URL("http://192.168.0.101/stream2stream/barca-rm-2009/2.ts"));
    secondHalf.setEventFileId(Long.MAX_VALUE - 2);

    return
            EventFileSource
                    .builder()
                    .channel("Test Channel")
                    .resolution(R_1080p)
                    .languages(List.of("Spanish"))
                    .eventFiles(List.of(firstHalf, secondHalf))
                    .build();
  }

  @Test
  @DisplayName("Refresh data for a test EventFile")
  void refreshEventFileData() {

    // Refresh EventFile
    eventFileService.refreshEventFileData(testEventFileSrc, true);

    // Perform tests
    final List<EventFile> eventFiles = testEventFileSrc.getEventFiles();
    eventFiles.forEach(
        eventFile -> {
          Log.i(LOG_TAG, String.format("Checking EventFile: %s, internal URL: %s", eventFile, eventFile.getInternalUrl()));

          final FFmpegMetadata metadata = eventFile.getMetadata();
          assertThat(eventFile.getInternalUrl()).isNotNull();
          assertThat(metadata.getStreams().get(0).getDuration()).isGreaterThan(100);
        });
  }

  @AfterAll
  static void tearDown() {
    // Remove test user from repo
    Log.i(LOG_TAG, "Deleting test user: " + testUser);
    userRepo.delete(testUser);
  }
}

