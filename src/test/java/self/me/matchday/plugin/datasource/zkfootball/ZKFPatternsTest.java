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

package self.me.matchday.plugin.datasource.zkfootball;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.FileSize;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.Season;
import self.me.matchday.model.Team;
import self.me.matchday.util.Log;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validate ZKF patterns loading")
class ZKFPatternsTest {

  private static final String LOG_TAG = "ZKFPatternsTest";

  // Test constants
  private static final String TEST_TITLE = "La Liga 20/21 J5 - Barcelona vs Sevilla - 04/10/2020";

  private static ZKFPatterns zkfPatterns;

  @BeforeAll
  static void setUp(@Autowired final ZKFPatterns _zkfPatterns) {
    zkfPatterns = _zkfPatterns;
  }

  @Test
  void testCompetitionMatcher() {

    final Matcher competitionMatcher = zkfPatterns.getCompetitionMatcher(TEST_TITLE);
    assertThat(competitionMatcher.find()).isTrue();
  }

  @Test
  void testSeasonMatcher() {

    final Matcher seasonMatcher = zkfPatterns.getSeasonMatcher(TEST_TITLE);
    assertThat(seasonMatcher.find()).isTrue();

    // Parse data
    final int startYear = Integer.parseInt(seasonMatcher.group(1)) + 2_000;
    final int endYear = Integer.parseInt(seasonMatcher.group(2)) + 2_000;
    final Season actualSeason = new Season(startYear, endYear);
    final Season expectedSeason = new Season(2020, 2021);

    // test
    assertThat(actualSeason).isEqualTo(expectedSeason);
  }

  @Test
  void testFixtureMatcher() {

    final Matcher fixtureMatcher = zkfPatterns.getFixtureMatcher(TEST_TITLE);
    assertThat(fixtureMatcher.find()).isTrue();

    final int fixtureNum = Integer.parseInt(fixtureMatcher.group(6));
    final Fixture actualFixture = new Fixture(fixtureNum);
    final Fixture expectedFixture = new Fixture(5);
    Log.i(LOG_TAG, "Testing fixture: " + actualFixture);
    assertThat(actualFixture).isEqualTo(expectedFixture);
  }

  @Test
  void testDateMatcher() {

    final Matcher dateMatcher = zkfPatterns.getDateMatcher(TEST_TITLE);
    assertThat(dateMatcher.find()).isTrue();

    final LocalDate actualDate =
        LocalDate.parse(dateMatcher.group(1), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    final LocalDate expectedDate = LocalDate.of(2020, 10, 4);
    Log.i(LOG_TAG, "Testing event date: " + actualDate);
    assertThat(actualDate).isEqualTo(expectedDate);
  }

  @Test
  void testTeamsMatcher() {

    final Matcher teamsMatcher = zkfPatterns.getTeamsMatcher(TEST_TITLE);
    assertThat(teamsMatcher.find()).isTrue();

    //  Parse teams
    final Team actualHomeTeam = new Team(teamsMatcher.group(1));
    final Team actualAwayTeam = new Team(teamsMatcher.group(2));
    final Team expectedHomeTeam = new Team("Barcelona");
    final Team expectedAwayTeam = new Team("Sevilla");

    Log.i(
        LOG_TAG,
        String.format("Testing teams: home [%s], away [%s]", actualHomeTeam, actualAwayTeam));
    assertThat(actualHomeTeam).isEqualTo(expectedHomeTeam);
    assertThat(actualAwayTeam).isEqualTo(expectedAwayTeam);
  }

  @Test
  void testFilesizeMatcher() {

    final Matcher filesizeMatcher = zkfPatterns.getFilesizeMatcher("3.5 GB");
    assertThat(filesizeMatcher.find()).isTrue();

    final float gigabytes = Float.parseFloat(filesizeMatcher.group(1));
    final Long actualFileSize = FileSize.ofGigabytes(gigabytes);
    final Long expectedFileSize = FileSize.ofBytes(3_758_096_384L);

    Log.i(LOG_TAG, "Testing parsed filesize: " + actualFileSize);
    assertThat(actualFileSize).isEqualTo(expectedFileSize);
  }

  @Test
  void isMetadata() {

    final String TEST_STRING =
        "<b><b><span>channel:</span>&nbsp;ESPN</b><span><b><span>language:</span>&nbsp;<span><b>english</b></span></b></span><span><b><span>format:</span>&nbsp;720p 60FPS mkv</b></span><span><b><span>bitrate:</span>&nbsp;7 MB/sec</b></span><span><b><span>size:</span>&nbsp;6 GB</b></span></b>";
    //    Jsoup.parse(TEST_STRING);
    final boolean isMetadata = zkfPatterns.isMetadata(TEST_STRING);

    Log.i(LOG_TAG, "Testing String: " + TEST_STRING);
    assertThat(isMetadata).isTrue();
  }

  @Test
  void testResolutionMatcher() {

    final String TEST_STRING = "720p";
    final Matcher resolutionMatcher = zkfPatterns.getResolutionMatcher(TEST_STRING);

    Log.i(LOG_TAG, "Testing resolution parsing for String: " + TEST_STRING);
    assertThat(resolutionMatcher.find()).isTrue();
  }

  @Test
  void testFramerateMatcher() {

    final String TEST_STRING = "720p 60FPS mkv";
    final Matcher framerateMatcher = zkfPatterns.getFramerateMatcher(TEST_STRING);

    Log.i(LOG_TAG, "Testing framerate parsing for String: " + TEST_STRING);
    assertThat(framerateMatcher.find()).isTrue();
    final int actualFramerate = Integer.parseInt(framerateMatcher.group(1));
    final int expectedFramerate = 60;
    assertThat(actualFramerate).isEqualTo(expectedFramerate);
  }

  @Test
  void testContainerMatcher() {

    final String TEST_STRING = "720p 60FPS mkv";
    final Matcher containerMatcher = zkfPatterns.getContainerMatcher(TEST_STRING);

    Log.i(LOG_TAG, "Testing container parsing for String: " + TEST_STRING);
    assertThat(containerMatcher.find()).isTrue();
    final String actualContainer = containerMatcher.group(1);
    final String expectedContainer = "mkv";
    assertThat(actualContainer).isEqualTo(expectedContainer);
  }

  @Test
  void testMbpsMatcher() {

    final String TEST_STRING = "7 MB/sec";
    final Matcher mbpsMatcher = zkfPatterns.getMbpsMatcher(TEST_STRING);

    Log.i(LOG_TAG, "Testing Mbps matching with String: " + TEST_STRING);
    assertThat(mbpsMatcher.find()).isTrue();
  }

  @Test
  void testKbpsMatcher() {

    final String TEST_STRING = "4000kbps";
    final Matcher kbpsMatcher = zkfPatterns.getKbpsMatcher(TEST_STRING);

    Log.i(LOG_TAG, "Testing Kbps matching with String: " + TEST_STRING);
    assertThat(kbpsMatcher.find()).isTrue();
  }
}
