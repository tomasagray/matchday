/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.galataman;

import static self.me.matchday.feed.galataman.GalatamanPattern.START_OF_SOURCE;
import static self.me.matchday.feed.galataman.GalatamanPattern.isSourceData;
import static self.me.matchday.feed.galataman.GalatamanPattern.isVideoLink;

import com.google.gson.JsonObject;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import self.me.matchday.feed.BloggerPost;
import self.me.matchday.feed.IMatchFileSource;
import self.me.matchday.feed.IMatchSource;
import self.me.matchday.feed.galataman.GalatamanMatchFileSource.GalatamanMatchSourceBuilder;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.MD5;
import self.me.matchday.model.Match;
import self.me.matchday.model.Match.MatchBuilder;
import self.me.matchday.model.Season;
import self.me.matchday.model.Team;
import self.me.matchday.util.Log;

/**
 * Represents an individual post on the Galataman website. Extends the BloggerPost class, and
 * contains additional processors to extract metadata relevant to this website.
 *
 * @author tomas
 */
public final class GalatamanPost extends BloggerPost implements IMatchSource {
  private static final String LOG_TAG = "GalatamanPost";

  // Fields
  // -------------------------------------------------------------------------
  private final List<IMatchFileSource> matchFileSources;
  private Match match; // the Match represented by this Post

  // Constructor
  // -------------------------------------------------------------------------
  private GalatamanPost(GalatamanPostBuilder builder) {
    // Call superclass constructor
    super(builder);

    // Copy over parsed Galataman-specific content, making sure List is immutable
    this.matchFileSources = Collections.unmodifiableList(builder.sources);
    // Extract Match metadata
    this.match = new MatchDataParser(getTitle()).parse();
  }

  // Getters
  // -------------------------------------------------------------------------
  public List<IMatchFileSource> getMatchFileSources() {
    return this.matchFileSources;
  }

  // Overridden methods
  // -------------------------------------------------------------------------
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    // Add newly analyzed info to previous String output
    sb.append(super.toString()).append("\nSources:\n");
    // Add each source
    this.matchFileSources.forEach(sb::append);
    return sb.toString();
  }

  /**
   * Return Match metadata.
   *
   * @return The Match represented by this Post
   */
  @Override
  public Match getMatch() {
    return this.match;
  }

  // Builder
  // ---------------------------------------------------------------------------------
  /** Parses Galataman-specific content and constructs a fully-formed GalatamanPost object. */
  static class GalatamanPostBuilder extends BloggerPostBuilder {
    private final List<GalatamanMatchFileSource> sources = new ArrayList<>();

    // Constructor
    GalatamanPostBuilder(JsonObject bloggerPost) {
      super(bloggerPost);
    }

    /** Extracts match source data from this post. */
    private void parseMatchSources() {
      try {
        // DOM-ify HTML content for easy manipulation
        Document doc = Jsoup.parse(this.getContent());

        // Since the document is loosely structured, we will walk through
        // it using a token, starting at the first source and looking
        // for what we want along the way
        Element token = doc.getElementsMatchingOwnText(START_OF_SOURCE).first();

        // Search until the end of the Document
        while (token != null) {
          // When we find a source
          if (isSourceData.test(token)) {
            // Save HTML
            String html = token.html();
            // URLS for this source
            List<URL> urls = new ArrayList<>();

            // Now, continue searching, this time for links,
            // until the next source or the end of the HTML
            Element innerToken = token.nextElementSibling();
            while ((innerToken != null) && !(isSourceData.test(innerToken))) {
              // When we find a link to a video file
              if (isVideoLink.test(innerToken)) {
                // Extract href attribute & add it to our
                // source's list of URLs
                urls.add(new URL(innerToken.attr("href")));
              }

              // Advance inner token
              innerToken = innerToken.nextElementSibling();
            }

            // Parse data into file sources
            GalatamanMatchFileSource fileSources =
                new GalatamanMatchSourceBuilder(html, urls).build();

            // Add match source to object
            this.sources.add(fileSources);
          }

          // Advance the search token
          token = token.nextElementSibling();
        }

      } catch (MalformedURLException e) {
        // Something went wrong extracting a source link
        GalatamanPostParseException exception =
            new GalatamanPostParseException(
                "Could not extract source link from post:" + this.getBloggerPostID(), e);
        // Log the error
        Log.e(LOG_TAG, "Error parsing links from GalatamanPost", exception);
        // Rethrow
        throw exception;

      } catch (RuntimeException e) {
        // Wrap exception
        GalatamanPostParseException exception =
            new GalatamanPostParseException(
                "Error while parsing: " + this.getBloggerPostID() + " at: " + this.getLink(), e);
        // Log the error
        Log.e(LOG_TAG, "There was a problem parsing a Galataman post", exception);
        // Rethrow exception
        throw exception;
      }
    }

    /**
     * Build the GalatamanPost object
     *
     * @return GalatamanPost - A fully-formed post object
     */
    @Override
    public GalatamanPost build() {
      // Super methods
      // -----------------------------------------------
      // Mandatory fields
      parsePostID();
      parsePublished();
      parseTitle();
      parseLink();
      parseContent();
      // Optional fields
      parseLastUpdated();
      parseCategories();

      // Galataman-specific
      // -----------------------------------------------
      // Parse content
      parseMatchSources();

      // Construct a fully-formed GalatamanPost object
      return new GalatamanPost(this);
    }
  }

  /**
   * Extract Match metadata (e.g., Teams, Competition, Fixture, etc.) from Galataman data. This
   * class expects the title of the Post to be of the format: [Competition Season] - [Fixture] -
   * [Home vs Away] - [Date]
   */
  class MatchDataParser {
    private static final int MILLENNIUM = 2000;

    // Patterns
    // TODO: Change to use java.util.regex.Pattern & java.util.regex.Matcher
    private static final String PART_SEPARATOR = " - ";
    private static final String TEAM_SEPARATOR = " vs.? ";
    private static final String DATETIME_PATTERN = "d/MM/yyyy";
    private final DateTimeFormatter DATE_TIME_FORMATTER
        = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
    private static final String DATE_PATTERN = "\\d{2}/\\d{2}/\\d{4}";
    private static final String TEAMS_PATTERN =
        "([a-zA-Z\\u00C0-\\u017F\\s-])+ vs.? ([a-zA-Z\\u00C0-\\u017F\\s-])+";
    private static final String SEASON_PATTERN = ".*\\d{2}/?\\d{2}.*";

    private List<String> titleParts;
    private MD5 matchId;
    private Team homeTeam;
    private Team awayTeam;
    private Competition competition;
    private Season season;
    private Fixture fixture;
    private LocalDate date;

    MatchDataParser(@NotNull String title) {
      // Break title apart
      this.titleParts = Arrays.asList(title.split(PART_SEPARATOR));
      this.matchId = new MD5(title);
    }

    /**
     * Returns a properly-built Match object.
     *
     * @return The Match metadata extracted from the GalatamanPost data
     */
    Match parse() {
      // First, parse the first segment (competition, season info)
      parseCompetitionData(titleParts.get(0));

      // Iterate through title parts, parsing each one
      titleParts.forEach( part -> {
        // Date segment
        if(part.matches(DATE_PATTERN)) {
          date = LocalDate.parse(part, DATE_TIME_FORMATTER);
        } else if(part.matches(TEAMS_PATTERN)) {
          // Split into teams
          List<String> teams = Arrays.asList(part.split(TEAM_SEPARATOR));
          homeTeam = new Team(teams.get(0));
          awayTeam = new Team(teams.get(1));
        } else {
          // TODO: Improve competition/fixture title recognition
          fixture = new Fixture(part);
        }
      });

      // Return a new Match
      return new MatchBuilder()
          .setMatchID(matchId)
          .setHomeTeam(homeTeam)
          .setAwayTeam(awayTeam)
          .setCompetition(competition)
          .setSeason(season)
          .setFixture(fixture)
          .setDate(date)
          .build();
    }

    private void parseCompetitionData(@NotNull String competitionTitle) {
      // Patterns
      Pattern singleYearSeasonPattern = Pattern.compile("\\d{4}");
      Matcher singleYearMatcher = singleYearSeasonPattern.matcher(competitionTitle);
      Pattern splitYearSeasonPattern = Pattern.compile("\\d{2}/\\d{2}");
      Matcher splitYearMatcher = splitYearSeasonPattern.matcher(competitionTitle);

      // Season years
      int startYear = getPublished().getYear();
      int endYear = startYear + 1;

      try {
        // Format: ------- xxxx -------
        if(singleYearMatcher.find()) {
          // End year is signified in title
          endYear = Integer.parseInt(singleYearMatcher.group(0));   // get first (only) occurrence
          startYear = endYear - 1;
        // Format: -------- xx/xx ---------
        } else if(splitYearMatcher.find()) {
          // Both years signified in title; split
          String[] years = splitYearMatcher.group(0).split("/");
          startYear = Integer.parseInt(years[0]) + MILLENNIUM;
          endYear = Integer.parseInt(years[1]) + MILLENNIUM;
        }
      } catch (NumberFormatException | IndexOutOfBoundsException e) {
        Log.e(
            LOG_TAG,
            "Could not parse Season data for Post with title: " + titleParts.toString(),
            e);
      }

      competition = new Competition(competitionTitle);
      season = new Season(startYear, endYear);
    }
  }
}
