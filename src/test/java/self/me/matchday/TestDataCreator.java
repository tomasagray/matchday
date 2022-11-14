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

package self.me.matchday;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static self.me.matchday.model.video.PartIdentifier.FIRST_HALF;
import static self.me.matchday.model.video.PartIdentifier.POST_MATCH;
import static self.me.matchday.model.video.PartIdentifier.PRE_MATCH;
import static self.me.matchday.model.video.PartIdentifier.SECOND_HALF;
import static self.me.matchday.model.video.Resolution.R_1080p;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.api.service.CompetitionService;
import self.me.matchday.api.service.EventService;
import self.me.matchday.api.service.FileServerUserService;
import self.me.matchday.api.service.HighlightService;
import self.me.matchday.api.service.TeamService;
import self.me.matchday.db.VideoFileSrcRepository;
import self.me.matchday.db.VideoStreamLocatorPlaylistRepo;
import self.me.matchday.db.VideoStreamLocatorRepo;
import self.me.matchday.model.Competition;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Event;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.model.FileSize;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.Highlight;
import self.me.matchday.model.Match;
import self.me.matchday.model.PatternKit;
import self.me.matchday.model.PlaintextDataSource;
import self.me.matchday.model.Season;
import self.me.matchday.model.Team;
import self.me.matchday.model.video.SingleStreamLocator;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFilePack;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;
import self.me.matchday.plugin.fileserver.TestFileServerPlugin;
import self.me.matchday.util.JsonParser;
import self.me.matchday.util.ResourceFileReader;

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class TestDataCreator {

  private static final Logger logger = LogManager.getLogger(TestDataCreator.class);

  private static final String BASE_URL = "http://192.168.0.107:7000";
  private static final Random numGen = new Random();

  private final EventService eventService;
  private final VideoFileSrcRepository fileSrcRepository;
  private final HighlightService highlightService;
  private final CompetitionService competitionService;
  private final TeamService teamService;
  private final FileServerUserService userService;
  private final VideoStreamLocatorPlaylistRepo locatorPlaylistRepo;
  private final VideoStreamLocatorRepo streamLocatorRepo;

  @Autowired
  public TestDataCreator(
      EventService eventService,
      VideoFileSrcRepository fileSrcRepository,
      HighlightService highlightService,
      CompetitionService competitionService,
      TeamService teamService,
      FileServerUserService userService,
      VideoStreamLocatorPlaylistRepo locatorPlaylistRepo,
      VideoStreamLocatorRepo locatorRepo) {

    this.eventService = eventService;
    this.fileSrcRepository = fileSrcRepository;
    this.highlightService = highlightService;
    this.competitionService = competitionService;
    this.teamService = teamService;
    this.userService = userService;
    this.locatorPlaylistRepo = locatorPlaylistRepo;
    this.streamLocatorRepo = locatorRepo;
  }

  private static int getRandomNumber(int min, int max) {
    final int number = (int) ((Math.random() * (min - max)) + min);
    return Math.abs(number);
  }

  // ======================== DATA SOURCE ========================
  public DataSource<Match> readTestHtmlDataSource() throws IOException {
    final String filename = "data/datasource/test_html_blogger_datasource.json";
    return readTestDataSource(filename);
  }

  public DataSource<Match> readTestJsonDataSource() throws IOException {
    final String filename = "data/datasource/test_json_blogger_datasource.json";
    return readTestDataSource(filename);
  }

  private @NotNull DataSource<Match> readTestDataSource(@NotNull String filename)
      throws IOException {
    final String dataSourceJson = ResourceFileReader.readTextResource(filename);
    final Type type = new TypeReference<PlaintextDataSource<Match>>() {}.getType();
    final PlaintextDataSource<Match> testDataSource = JsonParser.fromJson(dataSourceJson, type);
    assertThat(testDataSource).isNotNull();

    // randomize IDs to avoid collisions
    testDataSource
        .getPatternKits()
        .forEach(patternKit -> patternKit.setId(ThreadLocalRandom.current().nextLong()));
    logger.info("Read test datasource:\n{}", testDataSource);
    return testDataSource;
  }

  public @NotNull PatternKit<Event> createEventPatternKitManually(@NotNull String regex) {

    final PatternKit<Event> patternKit = new PatternKit<>(Event.class);
    patternKit.setPattern(Pattern.compile(regex, Pattern.UNICODE_CASE));

    Map<Integer, String> indexes = new HashMap<>();
    indexes.put(1, "competition");
    indexes.put(2, "season");
    indexes.put(3, "fixture");
    indexes.put(4, "date");
    indexes.put(5, "homeTeam");
    indexes.put(6, "awayTeam");
    patternKit.setFields(indexes);
    return patternKit;
  }

  public PatternKit<Event> createEventPatternKitFromFile() throws IOException {

    final String patternKitData =
        ResourceFileReader.readTextResource("data/test_event_pattern_kit.json");
    final Type type = new TypeToken<PatternKit<Event>>() {}.getType();
    return JsonParser.fromJson(patternKitData, type);
  }

  @NotNull
  public PatternKit<VideoFileSource> createFileSourcePatternKitManually() {

    final Pattern pattern =
        Pattern.compile(
            "Channel[\\s\\p{L}]*:? ([\\p{L}\\s+-]*) Source[\\p{L}\\s]*:? ([\\p{L}\\d-]*) "
                + "Language[\\p{L}\\s]*:? ([\\p{L}\\s./]*) Video[\\p{L}\\s]*:? (\\d+) [KkMmbps]* "
                + "‖ (\\p{L}\\.\\d+) (\\p{L}+) ‖ (\\d+)[fps]* Audio[\\p{L}\\s]*:? (\\d+)[\\sKkMmbps]+ "
                + "‖ ([\\p{L}\\d]+) ‖ ([\\d.]+) [chanelstro]* Duration[\\p{L}\\s]*:? (\\d+\\p{L}*) "
                + "Size[\\p{L}\\s]*:? ~?(\\d+)[GgMmBb]* Release[\\p{L}\\s]*:? [HhDd]* (\\d+[pi])",
            Pattern.UNICODE_CASE);
    final Map<Integer, String> indexes =
        new HashMap<>(
            Map.of(
                1,
                "channel",
                2,
                "source",
                3,
                "languages",
                4,
                "videoBitrate",
                5,
                "videoCodec",
                6,
                "mediaContainer",
                7,
                "framerate",
                8,
                "audioBitrate",
                9,
                "audioCodec",
                10,
                "audioChannels"));
    indexes.putAll(Map.of(11, "approximateDuration", 12, "filesize", 13, "resolution"));

    final PatternKit<VideoFileSource> patternKit = new PatternKit<>(VideoFileSource.class);
    patternKit.setPattern(pattern);
    patternKit.setFields(indexes);
    return patternKit;
  }

  public PatternKit<VideoFileSource> createFileSourcePatternFromFile() throws IOException {

    final String patternKitData =
        ResourceFileReader.readTextResource("data/datasource/test_filesource_pattern_kit.json");
    final Type type = new TypeToken<PatternKit<VideoFileSource>>() {}.getType();
    return JsonParser.fromJson(patternKitData, type);
  }

  // ================ EVENTS ======================

  public static void deleteGeneratedMatchArtwork(@NotNull Collection<Event> cleanupData)
      throws IOException {

    logger.info("Attempting to delete: {} test-generated files...", cleanupData.size());
    for (Event event : cleanupData) {
      final File file = event.getArtwork().getFile().toFile();
      logger.info("Deleting test-generated Artwork file: " + file);
      final boolean deleted = file.delete();
      if (deleted || !file.exists()) {
        logger.info("Successfully deleted file: " + file);
      } else {
        throw new IOException("Could not delete file: " + file);
      }
    }
  }

  @Transactional
  @NotNull
  public Match createTestMatch() {
    return this.createTestMatch("Test ");
  }

  public static String getRandomizedName(@NotNull String name, int start, int end) {
    final int seed = getRandomNumber(start, end);
    return String.format("Test Competition %s [%d]", name, seed);
  }

  @Transactional
  @NotNull
  public Match createTestMatch(@NotNull String name) {

    final Competition testCompetition =
        createTestCompetition("Competition " + getRandomizedName(name, 100, 1000));
    final Team homeTeam = createTestTeam("Home Team " + getRandomizedName(name, 10_000, 100_000));
    final Team awayTeam = createTestTeam("Away Team " + getRandomizedName(name, 10_000, 100_000));
    final Event testEvent =
        Match.builder()
            .date(LocalDateTime.now())
            .competition(testCompetition)
            .homeTeam(homeTeam)
            .awayTeam(awayTeam)
            .fixture(new Fixture(1))
            .season(new Season())
            .build();

    // Create file source & event files
    final VideoFileSource testFileSource = createVideoFileSource();
    testEvent.getFileSources().add(testFileSource);
    logger.info("Created test Event: {}", testEvent);
    return (Match) eventService.save(testEvent);
  }

  @Transactional
  public void deleteTestEvent(Event event) {
    logger.info("Deleting Event: {}", event);
    if (event != null && event.getEventId() != null) {
      eventService.delete(event.getEventId());
    }
    // if id == null, Event is not managed
  }

  @Transactional
  @NotNull
  public Competition createTestCompetition() {
    final String name = "TEST COMPETITION " + numGen.nextInt();
    return this.createTestCompetition(name);
  }

  @Transactional
  @NotNull
  public Competition createTestCompetition(@NotNull String name) {
    final Competition competition = new Competition(name);
    logger.info("Created test competition: {}", competition);
    return competitionService.save(competition);
  }

  @Transactional
  public void deleteTestCompetition(Competition competition) throws IOException {
    logger.info("Deleting test Competition: {}", competition);
    competitionService.delete(competition.getId());
  }

  @Transactional
  @NotNull
  public Team createTestTeam() {
    return this.createTestTeam("TEST TEAM " + numGen.nextInt());
  }

  @Transactional
  @NotNull
  public Team createTestTeam(@NotNull String name) {
    final Team team = new Team(name);
    return teamService.save(team);
  }

  @Transactional
  public void deleteTestTeam(@NotNull Team team) {
    logger.info("Deleting test Team: {}", team);
    teamService.deleteTeamByName(team.getName().getName());
  }

  @Transactional
  @NotNull
  public VideoFileSource createVideoFileSourceAndSave() {
    return fileSrcRepository.saveAndFlush(createVideoFileSource());
  }

  public VideoFileSource createVideoFileSource() {
    final int fileSetCount = 1;

    final List<VideoFilePack> videoFilePacks = createTestVideoFiles(fileSetCount);
    return VideoFileSource.builder()
        //        .fileSrcId(UUID.randomUUID())
        .channel("Test Channel")
        .resolution(R_1080p)
        .languages("English")
        .videoBitrate(8_000L)
        .videoFilePacks(videoFilePacks)
        .filesize(FileSize.ofGigabytes(8))
        .build();
  }

  @Transactional
  public void deleteVideoFileSource(@NotNull final VideoFileSource fileSource) {
    logger.info("Deleting VideoFileSource: {}", fileSource);
    fileSrcRepository.delete(fileSource);
  }

  @Transactional
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

    final VideoFileSource fileSource = createVideoFileSourceAndSave();
    final Path locatorPath = Path.of("C:\\Users\\Public\\Matchday\\testing");
    final VideoStreamLocatorPlaylist playlist =
        new VideoStreamLocatorPlaylist(fileSource, locatorPath);
    final VideoFilePack videoFiles = fileSource.getVideoFilePacks().get(0);
    videoFiles.forEachVideoFile(
        (title, file) -> {
          final VideoStreamLocator streamLocator = createStreamLocator(file);
          playlist.addStreamLocator(streamLocator);
        });

    final VideoStreamLocatorPlaylist locatorPlaylist = locatorPlaylistRepo.saveAndFlush(playlist);
    logger.info("Created VideoStreamLocatorPlaylist:\n{}", locatorPlaylist);
    return locatorPlaylist;
  }

  @Transactional
  public @NotNull VideoStreamLocator createStreamLocator(final VideoFile videoFile) {
    final VideoStreamLocator locator = new SingleStreamLocator();
    locator.setVideoFile(videoFile);
    final VideoStreamLocator streamLocator = streamLocatorRepo.saveAndFlush(locator);
    logger.info("Created VideoStreamLocator: {}", streamLocator);
    return streamLocator;
  }

  public @Nullable URL getPreMatchUrl() {
    return getTestUrl(
        BASE_URL + "/data/Euro_2020_-_France_vs._Switzerland/20210628-FRA-SWI-EK20_ETPEN.ts");
  }

  public @Nullable URL getFirstHalfUrl() {
    return getTestUrl(
        BASE_URL + "/data/Euro_2020_-_France_vs._Switzerland/20210628-FRA-SWI-EK20_1EN.ts");
  }

  public @Nullable URL getSecondHalfUrl() {
    return getTestUrl(
        BASE_URL + "/data/Euro_2020_-_France_vs._Switzerland/20210628-FRA-SWI-EK20_2EN.ts");
  }

  public @Nullable URL getPostMatchUrl() {
    return getTestUrl(
        BASE_URL + "/data/Euro_2020_-_France_vs._Switzerland/20210628-FRA-SWI-EK20_ETPEN.ts");
  }

  private @Nullable URL getTestUrl(@NotNull final String url) {
    try {
      final String seed = "?rSeed=" + UUID.randomUUID();
      return new URL(url + seed);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  // ====================== FILE SERVER ==============================

  @Transactional
  @NotNull
  public FileServerUser createTestFileServerUser() {

    // ensure different userdata each time
    final String username = String.format("user-%s@server.com", numGen.nextInt(Integer.MAX_VALUE));
    final String password = String.format("password-%s", numGen.nextInt(Integer.MAX_VALUE));
    final FileServerUser user =
        new FileServerUser(username, password, TestFileServerPlugin.pluginId);
    //    user.setLoggedIntoServer(TestFileServerPlugin.pluginId, new ArrayList<>());
    return userService.login(user);
  }

  @Transactional
  public Highlight createHighlightShow() {

    // Create test highlight show
    //    final String title = "Test Highlight Show " + numGen.nextInt();
    final Competition testCompetition = createTestCompetition();
    final Fixture testFixture = new Fixture(numGen.nextInt(34));
    final Season testSeason = new Season();

    final Highlight highlight =
        Highlight.highlightBuilder()
            .competition(testCompetition)
            .fixture(testFixture)
            .season(testSeason)
            .date(LocalDateTime.now())
            .build();
    return highlightService.save(highlight);
  }
}
