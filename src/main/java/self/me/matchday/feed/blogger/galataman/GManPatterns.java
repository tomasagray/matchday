package self.me.matchday.feed.blogger.galataman;

import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;
import self.me.matchday.fileserver.inclouddrive.ICDData;

/**
 * Container class for Galataman parsing patterns
 */
final class GManPatterns {

  // Entry parsing patterns
  static final String START_OF_SOURCE = Pattern.compile("__*").pattern();
  static final String METADATA_ITEM_DELIMITER =
      Pattern.compile("<span style=\"color: blue;\">(\\[)?").pattern();
  static final String METADATA_KV_DELIMITER =
      Pattern.compile("(])?</span>:(<span [^>]*>)?").pattern();
  static final String LANGUAGE_DELIMITER = Pattern.compile("[\\d.* ]|/").pattern();
  static final String AV_DATA_DELIMITER = Pattern.compile("‖").pattern();

  // Predicates
  static boolean isSourceData(@NotNull final Element elem) {
    return ("b".equals(elem.tagName())) && (elem.text().contains("Channel"));
  }

  static boolean isVideoLink(@NotNull final Element elem) {
    return
        ("a".equals(elem.tagName()))
            && (ICDData.getUrlMatcher(elem.attr("href")).find());
  }
}
