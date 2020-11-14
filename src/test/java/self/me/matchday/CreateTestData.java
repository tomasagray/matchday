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

package self.me.matchday;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.model.*;
import self.me.matchday.plugin.fileserver.FileServerPlugin;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static self.me.matchday.model.EventFileSource.Resolution.R_1080p;

public class CreateTestData {

  private static final String LOG_TAG = "CreateTestData";

  public static Match createTestMatch() {
    // Create & save test match & EventFileSource
    final Competition testCompetition = createTestCompetition();
    final Team testTeam = createTestTeam();
    final Match testMatch =
        new Match.MatchBuilder()
            .setDate(LocalDateTime.now())
            .setCompetition(testCompetition)
            .setHomeTeam(testTeam)
            .build();

    // Create file source & event files
    final EventFileSource testFileSource = createTestEventFileSource();
    testFileSource.getEventFiles().addAll(createTestEventFiles());
    testMatch.getFileSources().add(testFileSource);

    Log.i(LOG_TAG, "Created test Event: " + testMatch);
    return testMatch;
  }

  @NotNull
  public static Competition createTestCompetition() {
    return new Competition("TEST COMPETITION");
  }

  @NotNull
  public static Team createTestTeam() {
    return new Team("TEST TEAM");
  }

  public static EventFileSource createTestEventFileSource() {
    final EventFileSource fileSource = EventFileSource.builder()
            .channel("Event Service Test Channel")
            .resolution(R_1080p)
            .languages("English")
            .bitrate(8_000L)
            .fileSize(FileSize.ofGigabytes(8))
            .build();
    fileSource.getEventFiles().addAll(createTestEventFiles());
    return fileSource;
  }

  public static @NotNull List<EventFile> createTestEventFiles() {

    try {
      final URL firstHalfUrl = new URL("http://192.168.0.101/stream2stream/barca-rm-2009/1.ts");
      final URL secondHalfUrl = new URL("http://192.168.0.101/stream2stream/barca-rm-2009/2.ts");

      final EventFile preMatch = new EventFile(EventFile.EventPartIdentifier.PRE_MATCH, firstHalfUrl);
      final EventFile firstHalf = new EventFile(EventFile.EventPartIdentifier.FIRST_HALF, secondHalfUrl);
      final EventFile secondHalf =
          new EventFile(EventFile.EventPartIdentifier.SECOND_HALF, firstHalfUrl);
      final EventFile postMatch = new EventFile(EventFile.EventPartIdentifier.POST_MATCH, firstHalfUrl);

      return List.of(preMatch, firstHalf, secondHalf, postMatch);
    } catch (MalformedURLException e) {
      e.printStackTrace();
      Log.i(LOG_TAG, "URL was invalid! event files will be empty");
      return List.of();
    }
  }

  /** Test implementation of file server plugin */
  public static class TestFileServerPlugin implements FileServerPlugin {

    private static final FileServerUser testFileServerUser = createTestFileServerUser();

    private TestFileServerPlugin() {}

    private final UUID pluginId = UUID.randomUUID();
    private final Pattern urlPattern =
        Pattern.compile("http[s]?://192.168.0.101/stream2stream/[\\w./-]*");

    @Override
    public @NotNull ClientResponse login(@NotNull FileServerUser user) {
      return user.equals(testFileServerUser)
          ? ClientResponse.create(HttpStatus.OK).build()
          : ClientResponse.create(HttpStatus.UNAUTHORIZED).build();
    }

    @Override
    public boolean acceptsUrl(@NotNull URL url) {
      return urlPattern.matcher(url.toString()).find();
    }

    @Override
    public @NotNull Duration getRefreshRate() {
      return Duration.ofDays(1_000);
    }

    @Override
    public Optional<URL> getDownloadURL(@NotNull URL url, @NotNull Collection<HttpCookie> cookies) {
      return Optional.of(url);
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
      return "Test file server plugin";
    }
  }

  public static FileServerUser createTestFileServerUser() {
    return new FileServerUser("user", "password");
  }

  public static TestFileServerPlugin createTestFileServerPlugin() {
    return new TestFileServerPlugin();
  }
}
