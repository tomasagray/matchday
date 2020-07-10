package self.me.matchday.plugin.datasource.galataman;

import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;

/**
 * Container class for Galataman parsing patterns
 */
public final class GManPatterns {

  // Entry parsing patterns
  static final String START_OF_SOURCE = Pattern.compile("__*").pattern();
  static final String METADATA_ITEM_DELIMITER =
      Pattern.compile("<span style=\"color: blue;\">(\\[)?").pattern();
  static final String METADATA_KV_DELIMITER =
      Pattern.compile("(])?</span>:(<span [^>]*>)?").pattern();
  static final String LANGUAGE_DELIMITER = Pattern.compile("[\\d.* ]|/").pattern();
  static final String AV_DATA_DELIMITER = Pattern.compile("‖").pattern();

  // Event patterns
  public static final Pattern COMP_PATTERN = Pattern.compile("^([\\w\\s])+ ");
  public static final Pattern FIXTURE_PATTERN =
      Pattern.compile("((Semi-)?Final)|((J|(Matchday ))\\d+)");
  public static final Pattern TEAMS_PATTERN = Pattern.compile("(?U)([\\w ?]+) vs.? ([\\w ?]+)");
  public static final Pattern DATE_PATTERN = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

  // File link pattern
  private static final Pattern FILE_URL_PATTERN =
      Pattern.compile("https://www.inclouddrive.com/file/.*");

  // Predicates
  static boolean isSourceData(@NotNull final Element elem) {
    return ("b".equals(elem.tagName())) && (elem.text().contains("Channel"));
  }

  static boolean isVideoLink(@NotNull final Element elem) {

    final String href = elem.attr("href");
    final Matcher matcher = FILE_URL_PATTERN.matcher(href);
    return
        ("a".equals(elem.tagName())) && (matcher.find());
  }
}
