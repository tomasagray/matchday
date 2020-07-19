package self.me.matchday.plugin.datasource.zkfootball;

import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Container class for variables specific to the ZKFootball Blogger blog, needed for parsing.
 */
public class ZKFPatterns {

  // Selects the container elements for Event metadata
  public static final Pattern METADATA_PATTERN = Pattern
      .compile("(channel:)|(language:)|(format:)|(bitrate:)|(size:)");

  // Format patterns
  public static final Pattern resolutionPattern =
      Pattern.compile("(720|1080)[pi]", Pattern.CASE_INSENSITIVE);
  public static final Pattern frameRatePattern =
      Pattern.compile("(\\d+)(fps)", Pattern.CASE_INSENSITIVE);
  public static final Pattern containerPattern =
      Pattern.compile("mkv|ts", Pattern.CASE_INSENSITIVE);
  public static final Pattern FILE_SIZE_PATTERN =
      Pattern.compile("([\\d.,]+) ([gmk]b)", Pattern.CASE_INSENSITIVE);

  // Bitrate patterns
  public static final Long DEFAULT_BITRATE = 4_000_000L;
  public static final Pattern mbpsPattern =
      Pattern.compile("mb/sec", Pattern.CASE_INSENSITIVE);
  public static final Pattern kbpsPattern =
      Pattern.compile("kbps", Pattern.CASE_INSENSITIVE);

  // Event patterns
  public static final Pattern COMP_PATTERN = Pattern.compile("^([\\w\\s])+ ");
  public static final Pattern SEASON_PATTERN = Pattern.compile(" (\\d{2,4})/(\\d{2,4}) ");
  public static final Pattern FIXTURE_PATTERN =
      Pattern.compile("((Semi-)?Final)|((J|(Matchday ))\\d+)");
  public static final Pattern TEAMS_PATTERN = Pattern.compile("(?U)([\\w ?]+) vs.? ([\\w ?]+)");

  /**
   * Does the supplied text String contain EventFileSource metadata?
   *
   * @param text HTML text containing ZKF EventFileSource metadata
   * @return True / false
   */
  public static boolean isMetadata(@NotNull final String text) {
    return
        METADATA_PATTERN.matcher(text).find();
  }
}
