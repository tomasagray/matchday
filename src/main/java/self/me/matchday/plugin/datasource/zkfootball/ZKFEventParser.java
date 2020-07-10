package self.me.matchday.plugin.datasource.zkfootball;

import static self.me.matchday.plugin.datasource.zkfootball.ZKFPatterns.COMP_PATTERN;
import static self.me.matchday.plugin.datasource.zkfootball.ZKFPatterns.FIXTURE_PATTERN;
import static self.me.matchday.plugin.datasource.zkfootball.ZKFPatterns.SEASON_PATTERN;
import static self.me.matchday.plugin.datasource.zkfootball.ZKFPatterns.TEAMS_PATTERN;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import self.me.matchday.plugin.datasource.EventParser;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.HighlightShow.HighlightShowBuilder;
import self.me.matchday.model.Match.MatchBuilder;
import self.me.matchday.model.Season;
import self.me.matchday.model.Team;

/**
 * Implementation of the Event data parser interface for the ZKFootball blog, found at:
 * https://zkfootballmatch.blogspot.com
 */
public class ZKFEventParser implements EventParser {

  private final String title;
  // Event components
  private final Competition competition;
  private final Season season;
  private final Fixture fixture;
  private final LocalDateTime date;
  private Team homeTeam;
  private Team awayTeam;

  public ZKFEventParser(@NotNull final String title, @NotNull final LocalDateTime date) {

    this.title = title;
    this.date = date;
    // Parse title data
    this.competition = parseCompetition();
    this.season = parseSeason();
    this.fixture = parseFixture();
    parseTeams();
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
              .setTitle(title)
              .setCompetition(competition)
              .setSeason(season)
              .setFixture(fixture)
              .setDate(date)
              .build();
    }
  }

  private @Nullable Competition parseCompetition() {

    final Matcher matcher = COMP_PATTERN.matcher(title);
    return
        matcher.find() ? new Competition(matcher.group().trim()) : null;
  }

  private Season parseSeason() {

    Season result = new Season();
    final Matcher matcher = SEASON_PATTERN.matcher(title);
    try {
      if (matcher.find()) {
        final int startYear = fixYear(Integer.parseInt(matcher.group(1)));
        final int endYear = fixYear(Integer.parseInt(matcher.group(2)));
        result = new Season(startYear, endYear);
      }
    } catch (NumberFormatException ignore) {}

    return result;
  }

  private Fixture parseFixture() {

    // Result container
    Fixture result = null;
    final Matcher matcher = FIXTURE_PATTERN.matcher(title);

    try {
      if (matcher.find()) {
        if (matcher.group(1) != null) {
          result = new Fixture(matcher.group(1));
        } else if (matcher.group(3) != null && matcher.group(4) != null) {
          final String str = matcher.group(3).replace(matcher.group(4), "");
          final int i = Integer.parseInt(str);
          result = new Fixture(i);
        }
      }
    } catch (NumberFormatException ignore) {}
    return result;
  }

  private void parseTeams() {

    final Matcher matcher = TEAMS_PATTERN.matcher(title);
    if (matcher.find()) {
      homeTeam = new Team(matcher.group(1).trim());
      awayTeam = new Team(matcher.group(2).trim());
    }
  }

}
