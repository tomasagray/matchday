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
import self.me.matchday.model.*;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

import static self.me.matchday.model.EventFileSource.Resolution.R_1080p;

public class CreateTestData {

  private static final String LOG_TAG = "CreateTestData";

  public static URL FIRST_HALF_URL;
  public static URL SECOND_HALF_URL;
  public static URL GMAN_HTML;
  public static URL ZKF_JSON_URL;
  public static URL NITROFLARE_DL_URL;

  public static final Pattern URL_PATTERN =
      Pattern.compile("http[s]?://192.168.0.101/matchday-testing/[\\w./-]*");

  // initialize URLs
  static {
    try {
      FIRST_HALF_URL = new URL("http://192.168.0.101/matchday-testing/video/barca-rm-2009/1.ts");
      SECOND_HALF_URL = new URL("http://192.168.0.101/matchday-testing/video/barca-rm-2009/2.ts");
      GMAN_HTML = new URL("http://192.168.0.101/matchday-testing/gman.html");
      ZKF_JSON_URL = new URL("http://192.168.0.101/matchday-testing/zkf.json");
      NITROFLARE_DL_URL =
          new URL("http://192.168.0.101/matchday-testing/nitroflare_sample_download_page.htm");

    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }

  // ======================================== EVENTS =============================================================

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
    final EventFileSource fileSource =
        EventFileSource.builder()
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

    final EventFile preMatch =
        new EventFile(EventFile.EventPartIdentifier.PRE_MATCH, FIRST_HALF_URL);
    final EventFile firstHalf =
        new EventFile(EventFile.EventPartIdentifier.FIRST_HALF, SECOND_HALF_URL);
    final EventFile secondHalf =
        new EventFile(EventFile.EventPartIdentifier.SECOND_HALF, FIRST_HALF_URL);
    final EventFile postMatch =
        new EventFile(EventFile.EventPartIdentifier.POST_MATCH, FIRST_HALF_URL);

    return List.of(preMatch, firstHalf, secondHalf, postMatch);
  }


  // =============================================== FILE SERVER ===============================================

  public static FileServerUser createTestFileServerUser() {

    final Random r = new Random();
    // ensure different userdata each time
    final String username = String.format("user-%s@server.com", r.nextInt(Integer.MAX_VALUE));
    final String password = String.format("password-%s", r.nextInt(Integer.MAX_VALUE));
    return new FileServerUser(username, password);
  }
}
