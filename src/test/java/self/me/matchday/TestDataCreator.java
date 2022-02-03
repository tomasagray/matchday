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

import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.*;
import self.me.matchday.model.*;
import self.me.matchday.model.video.*;
import self.me.matchday.plugin.datasource.parsing.PatternKit;
import self.me.matchday.util.JsonParser;
import self.me.matchday.util.Log;
import self.me.matchday.util.ResourceFileReader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static self.me.matchday.model.video.PartIdentifier.*;
import static self.me.matchday.model.video.Resolution.R_1080p;

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class TestDataCreator {

  private static final String LOG_TAG = "CreateTestData";

  private static final String BASE_URL = "http://192.168.0.107:7000";
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

  // ======================== PatternKits ======================
  private static void writeTestPatternKit(@NotNull PatternKit<?> patternKit, String filename) {

    final String dataDir =
        "C:\\Users\\Tomas\\Projects\\Source\\IdeaProjects\\matchday\\src\\test\\resources\\";
    try (final BufferedWriter writer = new BufferedWriter(new FileWriter(dataDir + filename))) {
      final Type type = TypeToken.get(patternKit.getClass()).getType();
      final String json = JsonParser.toJson(patternKit, type);
      writer.write(json);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // ======================== DATA SOURCE ========================
  public DataSource readTestHtmlDataSource() {
    final String filename = "test_html_blogger_datasource.json";
    return readTestDataSource(filename);
  }

  public DataSource readTestJsonDataSource() {
    final String filename = "test_json_blogger_datasource.json";
    return readTestDataSource(filename);
  }

  private DataSource readTestDataSource(@NotNull String filename) {
    final String dataSourceJson =
        ResourceFileReader.readTextResource(TestDataCreator.class, filename);
    final DataSource testDataSource = JsonParser.fromJson(dataSourceJson, DataSource.class);
    Log.i(LOG_TAG, "Read test datasource:\n" + testDataSource);
    return testDataSource;
  }

  public List<PatternKit<? extends Event>> createSeveralEventPatternKits() {

    final String firstRegex =
        "([\\p{L}\\d\\s]+) (\\d{2,4}/\\d{2,4})[-Matchdy\\s]*(\\d+)[\\s-]*([\\p{L}\\s-]+) "
            + "vs.? ([\\p{L}\\s-]+) - (\\d{2}/\\d{2}/\\d{2,4})";
    final String secondRegex =
        "(?:FÚTBOL:\\s)*([\\p{L}\\s]*)[\\s-]*(\\d{2}/\\d{2}/\\d{2,4}) "
            + "([\\p{L}\\s-]+) vs.? ([\\p{L}\\s-]+) _+";
    final String thirdRegex =
        "([\\p{L}\\d\\s]+) (\\d{2,4}\\/\\d{2,4}) - "
            + "(\\d{2}\\/\\d{2}\\/\\d{2,4}) ([\\p{L}\\s-]+) vs.? ([\\p{L}\\s-]+)";
    final PatternKit<Event> pk1 = createEventPatternKitManually(firstRegex);
    final PatternKit<Event> pk2 = createEventPatternKitManually(secondRegex);
    final PatternKit<Event> pk3 = createEventPatternKitManually(thirdRegex);

    return List.of(pk1, pk2, pk3);
  }

  public PatternKit<Event> createEventPatternKit() {

    final PatternKit<Event> patternKit;
    //    patternKit = createEventPatternKitManually("([\\p{L}\\d\\s]+)
    // (\\d{2,4}/\\d{2,4})[-Matchdy\\s]*(\\d+)[\\s-]*([\\p{L}\\s-]+) "
    //                + "vs.? ([\\p{L}\\s-]+) - (\\d{2}/\\d{2}/\\d{2,4})");
    //    writeTestPatternKit(patternKit, "test_event_pattern_kit.json");
    patternKit = createEventPatternKitFromFile();
    return patternKit;
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

  public PatternKit<Event> createEventPatternKitFromFile() {

    final String patternKitData =
        ResourceFileReader.readTextResource(TestDataCreator.class, "test_event_pattern_kit.json");
    final Type type = new TypeToken<PatternKit<Event>>() {}.getType();
    return JsonParser.fromJson(patternKitData, type);
  }

  public PatternKit<VideoFileSource> createFileSourcePatternKit() {

    final PatternKit<VideoFileSource> patternKit;
    //    patternKit = createFileSourcePatternKitManually();
    //    writeTestPatternKit(patternKit, "test_filesource_pattern_kit.json");
    patternKit = createFileSourcePatternFromFile();
    return patternKit;
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

  public PatternKit<VideoFileSource> createFileSourcePatternFromFile() {

    final String patternKitData =
        ResourceFileReader.readTextResource(
            TestDataCreator.class, "test_filesource_pattern_kit.json");
    final Type type = new TypeToken<PatternKit<VideoFileSource>>() {}.getType();
    return JsonParser.fromJson(patternKitData, type);
  }

  public PatternKit<VideoFilePack> createVideoFilePackPatternKit() {

    final PatternKit<VideoFile> patternKit = new PatternKit<>(VideoFile.class);
    final Pattern pattern =
        Pattern.compile(
            "^http[s]?://[\\p{L}.]*filefox.cc/[\\w]+/[\\w-]*.(mkv|ts)", Pattern.UNICODE_CASE);
    final Map<Integer, String> fields = new HashMap<>(Map.of(0, "externalUrl"));

    patternKit.setPattern(pattern);
    patternKit.setFields(fields);
    return new PatternKit<>(VideoFilePack.class);
  }

  public PatternKit<VideoFile> createVideoFilePatternKit() {

    final PatternKit<VideoFile> patternKit = new PatternKit<>(VideoFile.class);

    final Pattern pattern = Pattern.compile("[1st2ndFirSeco-]+ Half|[Postre]+-Match");
    final Map<Integer, String> fields = Map.of(0, "title");

    patternKit.setPattern(pattern);
    patternKit.setFields(fields);
    return patternKit;
  }

  public PatternKit<PartIdentifier> createPartIdentifierPatternKit() {

    final Pattern pattern = Pattern.compile("(Pre-|Post-|1st|First|2nd|Second|Trophy)");
    final PatternKit<PartIdentifier> patternKit = new PatternKit<>(PartIdentifier.class);
    final Map<Integer, String> fields = Map.of(1, "name");

    patternKit.setPattern(pattern);
    patternKit.setFields(fields);
    return patternKit;
  }

  public PatternKit<URL> createUrlPatternKit() {

    final Pattern pattern =
        Pattern.compile("^http[s]?://[\\p{L}.]*filefox.cc/[\\w]+/[\\w-]*.(mkv|ts)");
    final Map<Integer, String> fields = Map.of(0, "url");
    final PatternKit<URL> urlPatternKit = new PatternKit<>(URL.class);

    urlPatternKit.setPattern(pattern);
    urlPatternKit.setFields(fields);
    return urlPatternKit;
  }

  public List<? extends Event> getExpectedEntityParserTestEventData() {

    final Type type = new TypeToken<List<? extends Event>>() {}.getType();
    final String testDataJson =
        ResourceFileReader.readTextResource(
            TestDataCreator.class, "test_entity_parser_event_data.json");
    return JsonParser.fromJson(testDataJson, type);
  }

  public List<? extends VideoFileSource> getExpectedEntityParserFileSourceData() {

    final Type type = new TypeToken<List<? extends VideoFileSource>>() {}.getType();
    final String testData =
        ResourceFileReader.readTextResource(
            TestDataCreator.class, "test_entity_parser_filesource_data.json");
    return JsonParser.fromJson(testData, type);
  }

  // ================ EVENTS ======================

  @Transactional
  @NotNull
  public Match createTestMatch() {
    return this.createTestMatch("");
  }

  @Transactional
  @NotNull
  public Match createTestMatch(@NotNull String competitionName) {
    // Create & save test match & VideoFileSource
    final Competition testCompetition = createTestCompetition(competitionName);
    final Team testTeam = createTestTeam(competitionName);
    final Match testMatch =
        Match.matchBuilder()
            .date(LocalDateTime.now())
            .competition(testCompetition)
            .homeTeam(testTeam)
            .awayTeam(testTeam)
            .fixture(new Fixture(1))
            .season(new Season())
            .build();

    // Create file source & event files
    final VideoFileSource testFileSource = createTestVideoFileSource();
    testMatch.getFileSources().add(testFileSource);

    Log.i(LOG_TAG, "Created test Event: " + testMatch);
    return matchRepository.saveAndFlush(testMatch);
  }

  @Transactional
  public void deleteTestEvent(@NotNull final Event event) {
    Log.i(LOG_TAG, "Deleting Event: " + event);
    eventRepository.delete(event);
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
    Log.i(LOG_TAG, "Created test competition: " + competition);
    return competitionRepository.saveAndFlush(competition);
  }

  @Transactional
  public void deleteTestCompetition(Competition competition) {
    Log.i(LOG_TAG, "Deleting test Competition: " + competition);
    competitionRepository.delete(competition);
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
    return teamRepository.saveAndFlush(team);
  }


  @Transactional
  public void deleteTestTeam(@NotNull Team team) {
    Log.i(LOG_TAG, "Deleting test Team: " + team);
    teamRepository.delete(team);
  }

  @Transactional
  @NotNull
  public VideoFileSource createTestVideoFileSource() {

    final int fileSetCount = 1;

    final List<VideoFilePack> videoFilePacks = createTestVideoFiles(fileSetCount);
    final VideoFileSource fileSource =
        VideoFileSource.builder()
            .fileSrcId(UUID.randomUUID())
            .channel("Event Service Test Channel")
            .resolution(R_1080p)
            .languages("English")
            .videoBitrate(8_000L)
            .videoFilePacks(videoFilePacks)
            .filesize(FileSize.ofGigabytes(8))
            .build();
    return fileSrcRepository.saveAndFlush(fileSource);
  }

  @Transactional
  public void deleteVideoFileSource(@NotNull final VideoFileSource fileSource) {
    Log.i(LOG_TAG, "Deleting VideoFileSource: " + fileSource);
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
      final String seed = "?rSeed=" + MD5String.generate();
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
    final FileServerUser user = new FileServerUser(username, password);
    return userRepo.saveAndFlush(user);
  }

  @Transactional
  public void deleteFileServerUser(@NotNull final FileServerUser user) {
    Log.i(LOG_TAG, "Deleting FileServerUser: " + user);
    userRepo.delete(user);
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
    return highlightRepository.saveAndFlush(highlight);
  }
}
