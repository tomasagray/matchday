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
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.*;
import self.me.matchday.model.*;
import self.me.matchday.model.video.*;
import self.me.matchday.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static self.me.matchday.model.video.VideoFile.EventPartIdentifier.*;
import static self.me.matchday.model.video.VideoFileSource.Resolution.R_1080p;

@Component
public class TestDataCreator {

  private static final String LOG_TAG = "CreateTestData";
  private static final Random numGen = new Random();

  private final EventRepository eventRepository;
  private final MatchRepository matchRepository;
  private final HighlightRepository highlightRepository;
  private final CompetitionRepository competitionRepository;
  private final TeamRepository teamRepository;
  private final VideoFileSrcRepository fileSrcRepository;
  private final FileServerUserRepo userRepo;
  private final VideoStreamLocatorPlaylistRepo locatorPlaylistRepo;
  private final VideoStreamLocatorRepo streamLocatorRepo;

  @Autowired
  public TestDataCreator(
      @NotNull final EventRepository eventRepository,
      @NotNull final MatchRepository matchRepository,
      @NotNull final HighlightRepository highlightRepository,
      @NotNull final CompetitionRepository competitionRepository,
      @NotNull final TeamRepository teamRepository,
      @NotNull final VideoFileSrcRepository fileSrcRepository,
      @NotNull final FileServerUserRepo userRepo,
      @NotNull final VideoStreamLocatorPlaylistRepo locatorPlaylistRepo,
      @NotNull final VideoStreamLocatorRepo locatorRepo) {

    this.eventRepository = eventRepository;
    this.matchRepository = matchRepository;
    this.highlightRepository = highlightRepository;
    this.competitionRepository = competitionRepository;
    this.teamRepository = teamRepository;
    this.fileSrcRepository = fileSrcRepository;
    this.userRepo = userRepo;
    this.locatorPlaylistRepo = locatorPlaylistRepo;
    this.streamLocatorRepo = locatorRepo;
  }

  // ================ EVENTS ======================
  public @NotNull Match createTestMatch() {
    // Create & save test match & VideoFileSource
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
    final VideoFileSource testFileSource = createTestVideoFileSource();
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

  public @NotNull VideoFileSource createTestVideoFileSource() {

    final int fileSetCount = 1;

    final List<VideoFilePack> videoFilePacks = createTestVideoFiles(fileSetCount);
    final VideoFileSource fileSource =
        VideoFileSource.builder()
            .fileSrcId(MD5String.generate())
            .channel("Event Service Test Channel")
            .resolution(R_1080p)
            .languages("English")
            .videoBitrate(8_000L)
            .videoFilePacks(videoFilePacks)
            .fileSize(FileSize.ofGigabytes(8))
            .build();
    return fileSrcRepository.saveAndFlush(fileSource);
  }

  public void deleteVideoFileSource(@NotNull final VideoFileSource fileSource) {
    Log.i(LOG_TAG, "Deleting VideoFileSource: " + fileSource);
    fileSrcRepository.delete(fileSource);
  }

  public @NotNull List<VideoFilePack> createTestVideoFiles(final int count) {

    List<VideoFilePack> videoFiles = new ArrayList<>();

    for (int i = 0; i < count; ++i) {
      URL preMatchUrl = getPreMatchUrl();
      URL firstHalfUrl = getFirstHalfUrl();
      URL secondHalfUrl = getSecondHalfUrl();
      URL postMatchUrl = getPostMatchUrl();

      assertThat(preMatchUrl).isNotNull();
      assertThat(firstHalfUrl).isNotNull();
      assertThat(secondHalfUrl).isNotNull();
      assertThat(postMatchUrl).isNotNull();

      final VideoFile preMatch = new VideoFile(PRE_MATCH, preMatchUrl);
      final VideoFile firstHalf = new VideoFile(FIRST_HALF, firstHalfUrl);
      final VideoFile secondHalf = new VideoFile(SECOND_HALF, secondHalfUrl);
      final VideoFile postMatch = new VideoFile(POST_MATCH, postMatchUrl);
      final VideoFilePack pack = new VideoFilePack();
      pack.put(preMatch);
      pack.put(firstHalf);
      pack.put(secondHalf);
      pack.put(postMatch);
      videoFiles.add(pack);
    }
    return videoFiles;
  }

  @Transactional
  public @NotNull VideoStreamLocatorPlaylist createStreamLocatorPlaylist() {

    final VideoFileSource fileSource = createTestVideoFileSource();
    final Path locatorPath = Path.of("C:\\Users\\Public\\Matchday\\testing");
    final VideoStreamLocatorPlaylist playlist =
        new VideoStreamLocatorPlaylist(fileSource, locatorPath);
    final VideoFilePack videoFiles = fileSource.getVideoFilePacks().get(0);
    videoFiles.forEach(
        (title, file) -> {
          final VideoStreamLocator streamLocator = createStreamLocator(file);
          playlist.addStreamLocator(streamLocator);
        });

    final VideoStreamLocatorPlaylist locatorPlaylist = locatorPlaylistRepo.saveAndFlush(playlist);
    Log.i(LOG_TAG, "Created VideoStreamLocatorPlaylist:\n" + locatorPlaylist);
    return locatorPlaylist;
  }

  @Transactional
  public @NotNull VideoStreamLocator createStreamLocator(final VideoFile videoFile) {
    final VideoStreamLocator locator = new SingleStreamLocator();
    locator.setVideoFile(videoFile);
    final VideoStreamLocator streamLocator = streamLocatorRepo.saveAndFlush(locator);
    Log.i(LOG_TAG, "Created VideoStreamLocator: " + streamLocator);
    return streamLocator;
  }

  public @Nullable URL getPreMatchUrl() {
    return getTestUrl(
        "http://192.168.0.107:7000/matches/data/Euro_2020_-_France_vs._Switzerland/20210628-FRA-SWI-EK20_ETPEN.ts");
  }

  public @Nullable URL getFirstHalfUrl() {
    return getTestUrl(
        "http://192.168.0.107:7000/matches/data/Euro_2020_-_France_vs._Switzerland/20210628-FRA-SWI-EK20_1EN.ts");
  }

  public @Nullable URL getSecondHalfUrl() {
    return getTestUrl(
        "http://192.168.0.107:7000/matches/data/Euro_2020_-_France_vs._Switzerland/20210628-FRA-SWI-EK20_2EN.ts");
  }

  public @Nullable URL getPostMatchUrl() {
    return getTestUrl(
        "http://192.168.0.107:7000/matches/data/Euro_2020_-_France_vs._Switzerland/20210628-FRA-SWI-EK20_ETPEN.ts");
  }

  private @Nullable URL getTestUrl(@NotNull final String url) {
    try {
      final String seed = "?rSeed=" + MD5String.generate();
      return new URL(url + seed);
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
