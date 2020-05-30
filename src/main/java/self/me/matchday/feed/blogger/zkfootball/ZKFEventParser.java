package self.me.matchday.feed.blogger.zkfootball;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.HighlightShow.HighlightShowBuilder;
import self.me.matchday.feed.IEventParser;
import self.me.matchday.model.Match.MatchBuilder;
import self.me.matchday.model.Season;
import self.me.matchday.model.Team;
import self.me.matchday.util.Log;

/**
 * Implementation of the Event data parser interface for the ZKFootball blog, found at:
 * https://zkfootballmatch.blogspot.com
 */
public class ZKFEventParser implements IEventParser {

  private static final String LOG_TAG = "ZKFEventParser";

  // Part patterns
  private static final Pattern TITLE_SPLITTER = Pattern.compile(" - ");
  private static final Pattern SEASON_PATTERN = Pattern.compile("\\d{2,4}/\\d{2}");
  private static final Pattern COMP_SEASON_PATTERN =
      Pattern.compile("[\\w ]+ " + SEASON_PATTERN.pattern());
  private static final Pattern TEAMS_PATTERN = Pattern.compile("[\\w ]+ [Vv][Ss].? [\\w]+");
  private static final Pattern FIXTURE_PATTERN = Pattern
      .compile("([Ss][Ee][Mm][Ii]-)?([Ff][Ii][Nn][Aa][Ll])|(Matchday \\d*)");
  private static final Pattern TITLE_FIXTURE_PATTERN = Pattern.compile("J\\d{2}");

  // Part indices
  private int COMP_INDEX = -1;
  private int TEAM_INDEX = -1;
  private int FIXTURE_INDEX = -1;
  // Title part container
  private String[] titleParts;
  // Event components
  private Competition competition;
  private Team homeTeam;
  private Team awayTeam;
  private Season season;
  private Fixture fixture;
  private final LocalDateTime date;

  // Constructor
  public ZKFEventParser(@NotNull final String title, @NotNull final LocalDateTime date) {

    // Default Season & Fixture
    this.season = new Season(LocalDate.now().getYear(), LocalDate.now().getYear() + 1);
    this.fixture = new Fixture();
    // Set date
    this.date = date;

    // Parse title data
    parseEventData(title);
  }

  @Override
  public Event getEvent() {
    if (homeTeam != null && awayTeam != null) {
      return
          new MatchBuilder()
              .setHomeTeam(homeTeam)
              .setAwayTeam(awayTeam)
              .setCompetition(competition)
              .setSeason(season)
              .setFixture(fixture)
              .setDate(date)
              .build();
    } else {
      return
          new HighlightShowBuilder()
              .setTitle(String.join(TITLE_SPLITTER.pattern(), titleParts))
              .setCompetition(competition)
              .setSeason(season)
              .setFixture(fixture)
              .setDate(date)
              .build();
    }
  }

  /**
   * Break the title String into component parts, and call the index parser. Then call the title
   * parser.
   *
   * @param title The title String.
   */
  private void parseEventData(@NotNull final String title) {

    // Split title String into component parts
    titleParts = title.split(TITLE_SPLITTER.pattern());
    // Determine indices of Event metadata components in the title String
    parsePartIndices();
    // Parse Event data from title
    parseTitleParts();
  }

  /**
   * Determine which parts of the Post title contain which elements of Event metadata.
   */
  private void parsePartIndices() {
    for (int i = 0; i < titleParts.length; ++i) {
      final String titlePart = titleParts[i];
      if (COMP_SEASON_PATTERN.matcher(titlePart).find()) {
        COMP_INDEX = i;
      } else if (TEAMS_PATTERN.matcher(titlePart).find()) {
        TEAM_INDEX = i;
      } else if (FIXTURE_PATTERN.matcher(titlePart).find()) {
        FIXTURE_INDEX = i;
      }
    }
  }

  /**
   * Call the parser methods to parse each part of the title String in turn.
   */
  private void parseTitleParts() {
    // Competition
    parseCompetitionData();
    // Teams
    parseTeamData();
    // Fixture
    parseFixtureData();
  }

  /**
   * Parse Competition, Season & Fixture data from the title String.
   */
  private void parseCompetitionData() {

    if (COMP_INDEX != -1) {
      final int MILLENNIA = 2_000;
      int startYear = MILLENNIA, endYear = MILLENNIA;
      final String competition = titleParts[COMP_INDEX];

      // Parse Season data, if available
      try {
        final Matcher seasonMatcher = SEASON_PATTERN.matcher(competition);
        if (seasonMatcher.find()) {
          final String[] seasonSplit = seasonMatcher.group().split("/");
          int sy = Integer.parseInt(seasonSplit[0]);
          int ey = Integer.parseInt(seasonSplit[1]);
          // If years include millennia data, set start year; otherwise add
          startYear = (sy > 100) ? sy : startYear + sy;
          endYear = (ey > 100) ? ey : endYear + ey;
        }
      } finally {
        this.season = new Season(startYear, endYear);
      }

      // Parse Fixture data if available
      final Matcher fixtureMatcher = TITLE_FIXTURE_PATTERN.matcher(competition);
      if (fixtureMatcher.find()) {
        final int fixture = Integer.parseInt(fixtureMatcher.group().substring(1));
        this.fixture = new Fixture("Matchday", fixture);
      }

      // Finally, set competition to first section of part
      final Matcher competitionMatcher = SEASON_PATTERN.matcher(competition);
      if (competitionMatcher.find()) {
        final String substring = competition
            .substring(0, competition.indexOf(competitionMatcher.group()));
        this.competition = new Competition(substring);
      }

    } else {
      // default: Competition title is full first section of title
      this.competition = new Competition(titleParts[0]);
    }
  }

  /**
   * Parse Home & Away Team data from title String.
   */
  private void parseTeamData() {
    try {
      if (TEAM_INDEX != -1) {
        final String[] teams = titleParts[TEAM_INDEX].split(" [Vv][Ss].? ");
        this.homeTeam = new Team(teams[0]);
        this.awayTeam = new Team(teams[1]);
      }
    } catch (RuntimeException e) {
      Log.d(LOG_TAG, "Could not parse Team data from String: " + Arrays.toString(titleParts), e);
    }
  }

  /**
   * Parse Fixture data from title String. This will override any Fixture data set by the
   * parseCompetition() method.
   */
  private void parseFixtureData() {
    if (FIXTURE_INDEX != -1) {
      try {
        // find fixture number
        final String fixture = titleParts[FIXTURE_INDEX];
        final String[] split = fixture.split("\\d{2}");
        if (split.length > 1) {
          this.fixture = new Fixture(split[0], Integer.parseInt(split[1]));
        } else {
          this.fixture = new Fixture(fixture);
        }
      } catch (RuntimeException e) {
        Log.d(LOG_TAG, "Error parsing fixture data", e);
      }
    }
  }
}
