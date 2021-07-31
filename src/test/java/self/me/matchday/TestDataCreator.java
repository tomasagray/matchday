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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.me.matchday.db.*;
import self.me.matchday.model.*;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.util.Log;
import self.me.matchday.util.ResourceFileReader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static self.me.matchday.model.EventFileSource.Resolution.R_1080p;

@Component
public class TestDataCreator {

  private static final String LOG_TAG = "CreateTestData";
  private static final Random numGen = new Random();
  public static final String GMAN_HTML;
  public static final String ZKF_JSON;
  public static final URL NITROFLARE_DL_URL;

  static {
    try {
      GMAN_HTML =
          String.join(
              " ",
              ResourceFileReader.readTextResource(
                  TestDataCreator.class, "gman_sample_page_20210416.html"));
      ZKF_JSON =
          String.join(
              "",
              ResourceFileReader.readTextResource(
                  TestDataCreator.class, "zkf_sample_20210416.json"));
      NITROFLARE_DL_URL = new URL("https://www.nitroflare.com/");

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private final EventRepository eventRepository;
  private final MatchRepository matchRepository;
  private final HighlightRepository highlightRepository;
  private final CompetitionRepository competitionRepository;
  private final TeamRepository teamRepository;
  private final EventFileSrcRepository fileSrcRepository;
  private final FileServerUserRepo userRepo;

  @Autowired
  public TestDataCreator(
      @NotNull final EventRepository eventRepository,
      @NotNull final MatchRepository matchRepository,
      @NotNull final HighlightRepository highlightRepository,
      @NotNull final CompetitionRepository competitionRepository,
      @NotNull final TeamRepository teamRepository,
      @NotNull final EventFileSrcRepository fileSrcRepository,
      @NotNull final FileServerUserRepo userRepo) {

    this.eventRepository = eventRepository;
    this.matchRepository = matchRepository;
    this.highlightRepository = highlightRepository;
    this.competitionRepository = competitionRepository;
    this.teamRepository = teamRepository;
    this.fileSrcRepository = fileSrcRepository;
    this.userRepo = userRepo;
  }

  // ================ EVENTS ======================
  public @NotNull Match createTestMatch() {
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
    testMatch.getFileSources().add(testFileSource);

    Log.i(LOG_TAG, "Created test Event: " + testMatch);
    return matchRepository.saveAndFlush(testMatch);
  }

  public Highlight createHighlightShow() {

    // Create test highlight show
    final String title = "Test Highlight Show " + numGen.nextInt();
    final Competition testCompetition = createTestCompetition();
    final Fixture testFixture = new Fixture(numGen.nextInt(34));
    final Season testSeason = new Season();

    final Highlight highlight =
        new Highlight.HighlightBuilder()
            .setTitle(title)
            .setCompetition(testCompetition)
            .setFixture(testFixture)
            .setSeason(testSeason)
            .setDate(LocalDateTime.now())
            .build();
    return highlightRepository.saveAndFlush(highlight);
  }

  public void deleteTestEvent(@NotNull final Event event) {
    Log.i(LOG_TAG, "Deleting Event: " + event);
    eventRepository.delete(event);
  }

  @NotNull
  public Competition createTestCompetition() {
    final Competition competition = new Competition("TEST COMPETITION " + numGen.nextInt());
    return competitionRepository.saveAndFlush(competition);
  }

  @NotNull
  public Team createTestTeam() {
    final Team team = new Team("TEST TEAM " + numGen.nextInt());
    return teamRepository.saveAndFlush(team);
  }

  public @NotNull EventFileSource createTestEventFileSource() {

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
    return fileSrcRepository.saveAndFlush(fileSource);
  }

  public void deleteEventFileSource(@NotNull final EventFileSource fileSource) {
    Log.i(LOG_TAG, "Deleting EventFileSource: " + fileSource);
    fileSrcRepository.delete(fileSource);
  }

  public @NotNull List<EventFile> createTestEventFiles() {

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

  public @Nullable URL getPreMatchUrl() {
    try {
      final String seed = "?rSeed=" + MD5String.generate();
      return new URL(
          "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
              + seed);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  public @Nullable URL getFirstHalfUrl() {

    try {
      final String seed = "?rSeed=" + MD5String.generate();
      return new URL(
          "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
              + seed);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  public @Nullable URL getSecondHalfUrl() {

    try {
      final String seed = "?rSeed=" + MD5String.generate();
      return new URL(
          "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
              + seed);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  public @Nullable URL getPostMatchUrl() {

    try {
      final String seed = "?rSeed=" + MD5String.generate();
      return new URL(
          "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
              + seed);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  // ====================== FILE SERVER ==============================

  public @NotNull FileServerUser createTestFileServerUser() {

    // ensure different userdata each time
    final String username = String.format("user-%s@server.com", numGen.nextInt(Integer.MAX_VALUE));
    final String password = String.format("password-%s", numGen.nextInt(Integer.MAX_VALUE));
    final FileServerUser user = new FileServerUser(username, password);
    return userRepo.saveAndFlush(user);
  }

  public void deleteFileServerUser(@NotNull final FileServerUser user) {
    Log.i(LOG_TAG, "Deleting FileServerUser: " + user);
    userRepo.delete(user);
  }
}
