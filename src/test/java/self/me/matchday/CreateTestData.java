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

package self.me.matchday;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.*;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static self.me.matchday.model.EventFileSource.Resolution.R_1080p;

public class CreateTestData {

  public static final Pattern URL_PATTERN =
      Pattern.compile("http[s]?://192.168.0.101/matchday-testing/[\\w./-]*");
  private static final String LOG_TAG = "CreateTestData";
  private static final Random numGen = new Random();
  public static URL GMAN_HTML;
  public static URL ZKF_JSON_URL;
  public static URL NITROFLARE_DL_URL;

  static {
    try {
      GMAN_HTML = new URL("http://192.168.0.101/matchday-testing/gman.html");
      ZKF_JSON_URL = new URL("http://192.168.0.101/matchday-testing/zkf.json");
      NITROFLARE_DL_URL =
          new URL("http://192.168.0.101/matchday-testing/nitroflare_sample_download_page.htm");

    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }

  // ================ EVENTS ======================

  public static Match createTestMatch() {
    // Create & save test match & EventFileSource
    final Competition testCompetition = createTestCompetition();
    final Team testTeam = createTestTeam();
    final Match testMatch =
        new Match.MatchBuilder()
            .setDate(LocalDateTime.now())
            .setCompetition(testCompetition)
            .setHomeTeam(testTeam)
            .setAwayTeam(testTeam)
            .setFixture(new Fixture(1))
            .setSeason(new Season())
            .build();

    // Create file source & event files
    final EventFileSource testFileSource = createTestEventFileSource();
    testFileSource.getEventFiles().addAll(createTestEventFiles());
    testMatch.getFileSources().add(testFileSource);

    Log.i(LOG_TAG, "Created test Event: " + testMatch);
    return testMatch;
  }

  public static Highlight createHighlightShow() {

    // Create test highlight show
    final String title = "Test Highlight Show " + numGen.nextInt();
    final Competition testCompetition = createTestCompetition();
    final Fixture testFixture = new Fixture(numGen.nextInt(34));
    final Season testSeason = new Season();

    return new Highlight.HighlightBuilder()
        .setTitle(title)
        .setCompetition(testCompetition)
        .setFixture(testFixture)
        .setSeason(testSeason)
        .setDate(LocalDateTime.now())
        .build();
  }

  @NotNull
  public static Competition createTestCompetition() {
    return new Competition("TEST COMPETITION " + numGen.nextInt());
  }

  @NotNull
  public static Team createTestTeam() {
    return new Team("TEST TEAM " + numGen.nextInt());
  }

  public static EventFileSource createTestEventFileSource() {

    final EventFileSource fileSource =
        EventFileSource.builder()
            .eventFileSrcId(MD5String.generate())
            .channel("Event Service Test Channel")
            .resolution(R_1080p)
            .languages("English")
            .bitrate(8_000L)
            .fileSize(FileSize.ofGigabytes(8))
            .build();
    fileSource.getEventFiles().addAll(createTestEventFiles());
    return fileSource;
  }

  @SneakyThrows
  public static @NotNull List<EventFile> createTestEventFiles() {

    URL preMatchUrl = getPreMatchUrl();
    URL firstHalfUrl = getFirstHalfUrl();
    URL secondHalfUrl = getSecondHalfUrl();
    URL postMatchUrl = getPostMatchUrl();

    assertThat(preMatchUrl).isNotNull();
    assertThat(firstHalfUrl).isNotNull();
    assertThat(secondHalfUrl).isNotNull();
    assertThat(postMatchUrl).isNotNull();

    final EventFile preMatch = new EventFile(EventFile.EventPartIdentifier.PRE_MATCH, preMatchUrl);
    final EventFile firstHalf =
        new EventFile(EventFile.EventPartIdentifier.FIRST_HALF, firstHalfUrl);
    final EventFile secondHalf =
        new EventFile(EventFile.EventPartIdentifier.SECOND_HALF, secondHalfUrl);
    final EventFile postMatch =
        new EventFile(EventFile.EventPartIdentifier.POST_MATCH, postMatchUrl);

    return List.of(preMatch, firstHalf, secondHalf, postMatch);
  }

  public static URL getPreMatchUrl() {
    try {
      final String seed = "?rSeed=" + MD5String.generate();
      return new URL(
          "http://192.168.0.101/matchday-testing/video/tottenham-arsenal/20201206-TOT-ARS-EPL_0-1080.mkv"
              + seed);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static URL getFirstHalfUrl() {

    try {
      final String seed = "?rSeed=" + MD5String.generate();
      return new URL(
          "http://192.168.0.101/matchday-testing/video/tottenham-arsenal/20201206-TOT-ARS-EPL_1-1080.mkv"
              + seed);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static URL getSecondHalfUrl() {

    try {
      final String seed = "?rSeed=" + MD5String.generate();
      return new URL(
          "http://192.168.0.101/matchday-testing/video/tottenham-arsenal/20201206-TOT-ARS-EPL_2-1080.mkv"
              + seed);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static URL getPostMatchUrl() {

    try {
      final String seed = "?rSeed=" + MD5String.generate();
      return new URL(
          "http://192.168.0.101/matchday-testing/video/tottenham-arsenal/20201206-TOT-ARS-EPL_3-1080.mkv"
              + seed);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  // ====================== FILE SERVER ==============================

  public static FileServerUser createTestFileServerUser() {

    // ensure different userdata each time
    final String username = String.format("user-%s@server.com", numGen.nextInt(Integer.MAX_VALUE));
    final String password = String.format("password-%s", numGen.nextInt(Integer.MAX_VALUE));
    return new FileServerUser(username, password);
  }
}
