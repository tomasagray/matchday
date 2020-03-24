package self.me.matchday.feed.blogger.zkfootball;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import self.me.matchday.feed.IEventFileSourceParser;
import self.me.matchday.feed.blogger.InvalidBloggerPostException;
import self.me.matchday.fileserver.inclouddrive.ICDData;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFile.EventPartIdentifier;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventFileSource.Resolution;
import self.me.matchday.util.Log;

/**
 * Implementation of the Event File Source parser, specific to the ZKFootball blog, found at:
 * https://zkfootballmatch.blogspot.com
 */
public class ZKFEventFileSourceParser implements IEventFileSourceParser {

  private static final String LOG_TAG = "ZKEventFileSrcParser";

  // Default values
  private static final Long DEFAULT_BITRATE = 4_000_000L;

  // Metadata tags
  private static final String CHANNEL = "channel:";
  private static final String LANGUAGE = "language:";
  private static final String FORMAT = "format:";
  private static final String BITRATE = "bitrate:";
  private static final String SIZE = "size:";

  private final String html;  // Metadata html
  private final List<EventFileSource> eventFileSources = new ArrayList<>();

  // Constructor
  public ZKFEventFileSourceParser(@NotNull final String html) {
    this.html = html;
    parseEventFileSources();
  }

  @Override
  public List<EventFileSource> getEventFileSources() {
    return eventFileSources;
  }

  /**
   * Parse the HTML String which was passed to the constructor into
   */
  private void parseEventFileSources() {

    // DOM-ify HTML
    final Document dom = Jsoup.parse(this.html);
    // Find all top-level <span>'s
    final Elements spans = dom.select("div > span");
    // Examine each <span>
    spans.forEach(span -> {

      // Find links - must scan sequentially as sources loosely structured
      final Elements children = span.children();
      Element token = children.first();
      // until the end of document
      while (token != null) {

        // Containers
        EventPartIdentifier eventPartIdentifier = EventPartIdentifier.DEFAULT;
        EventFile eventFile;
        EventFileSource eventFileSource = new EventFileSource();

        // find next source - links (w/titles), metadata
        while (token != null && !(ZKPatterns.SOURCE_SEPARATOR.matcher(token.text()).find())) {

          // find part title
          if (EventPartIdentifier.isPartIdentifier(token.html())) {
            eventPartIdentifier = EventPartIdentifier.fromString(token.text());
          } else if (ZKPatterns.isVideoLink.test(token)) {
            // Create EventFile from link w/ above title
            try {
              eventFile = new EventFile(eventPartIdentifier, new URL(token.attr("href")));
              eventFileSource.getEventFiles().add(eventFile);
            } catch (MalformedURLException e) {
              Log.d(LOG_TAG, "Could not parse href: " + token.attr("href"));
            }
          } else if (ZKPatterns.isMetadata.test(token)) {
            // Populate EventFileSource metadata
            populateFileSourceMetadata(token.select(ZKPatterns.METADATA_SELECTOR), eventFileSource);
            // add complete file source to list
            eventFileSources.add(eventFileSource);
            // reset container
            eventFileSource = new EventFileSource();
          }
          token = token.nextElementSibling();
        }

        // advance token
        if (token != null) {
          token = token.nextElementSibling();
        }
      }
    });
  }

  /**
   * Populate an EventFileSource object with metadata
   *
   * @param elements        The collection of Jsoup Elements which contain metadata.
   * @param eventFileSource A populated EventFileSource object.
   */
  private void populateFileSourceMetadata(@NotNull final Elements elements,
      @NotNull final EventFileSource eventFileSource) {

    elements.forEach(element -> {
      // analyze metadata tag
      switch (element.text()) {
        case CHANNEL:
          eventFileSource.setChannel(cleanMetadata(element.nextSibling().toString()));
          break;

        case LANGUAGE:
          // get language from next span
          final String language = cleanMetadata(element.nextElementSibling().select("b").text());
          // capitalize first letter
          final String Language = language.substring(0, 1).toUpperCase() + language.substring(1);
          eventFileSource.getLanguages().add(Language);
          break;

        case FORMAT:
          // Format patterns
          final Pattern resolutionPattern = Pattern
              .compile("(720|1080)[pi]", Pattern.CASE_INSENSITIVE);
          final Pattern frameRatePattern = Pattern.compile("(\\d+)(fps)", Pattern.CASE_INSENSITIVE);
          final Pattern containerPattern = Pattern.compile("mkv|ts", Pattern.CASE_INSENSITIVE);

          // split format string
          final Node sibling = element.nextSibling();
          final String[] format = sibling.toString().split(" ");
          try {
            for (String part : format) {
              // Special 4k handler
              if (part.contains("4096x2160")) {
                eventFileSource.setResolution(Resolution.R_4k);
                // 720 & 1080
              } else if (resolutionPattern.matcher(part).find()) {
                eventFileSource.setResolution(Resolution.fromString(part));
              } else {
                final Matcher frMatcher = frameRatePattern.matcher(part);
                if (frMatcher.find()) {
                  // Parse frame rate
                  eventFileSource.setFrameRate(Integer.parseInt(frMatcher.group(1)));
                } else if (containerPattern.matcher(part).find()) {
                  eventFileSource.setMediaContainer(part.toUpperCase());
                }
              }
            }
          } catch (RuntimeException e) {
            Log.d(LOG_TAG, "Could not parse format data from Element: " + sibling, e);
          }
          break;

        case BITRATE:
          // Bitrate patterns
          final Pattern mbpsPattern = Pattern.compile("mb/sec", Pattern.CASE_INSENSITIVE);
          final Pattern kbpsPattern = Pattern.compile("kbps", Pattern.CASE_INSENSITIVE);

          // Parse bitrate
          final Node bitrateNode = element.nextSibling();
          try {
            final String bitrate = cleanMetadata(bitrateNode.toString()).trim();
            // Split digit from unit
            final Matcher bitrateMatcher = Pattern.compile("(\\d+)").matcher(bitrate);
            if (bitrateMatcher.find()) {
              final int digit = Integer.parseInt(bitrateMatcher.group());
              // Parse conversion factor
              if (mbpsPattern.matcher(bitrate).find()) {
                eventFileSource.setBitrate(digit * 1_000_000L);
              } else if (kbpsPattern.matcher(bitrate).find()) {
                eventFileSource.setBitrate(digit * 1_000L);
              }
            }
          } catch (NumberFormatException e) {
            Log.d(LOG_TAG, "Error parsing bitrate data", e);
          } finally {
            // Ensure bitrate has been set
            if (eventFileSource.getBitrate() == null) {
              Log.d(LOG_TAG, String.format(
                  "Could not parse EventFileSource bitrate from String: %s; defaulting to 4MBps",
                  bitrateNode));
              eventFileSource.setBitrate(DEFAULT_BITRATE);
            }
          }
          break;

        case SIZE:
          eventFileSource.setApproximateFileSize(cleanMetadata(element.nextSibling().toString()));
          break;

        default:
          throw new InvalidBloggerPostException("Could not parse metadata item: " + element);
      }
    });
  }

  /**
   * Remove non-breaking spaces & excess whitespace
   *
   * @param metadata The string to be cleaned
   * @return A clean String.
   */
  @NotNull
  private String cleanMetadata(@NotNull final String metadata) {
    return metadata.replace("&nbsp;", "").trim();
  }

  /**
   * Container class for variables specific to the ZKFootball Blogger blog, needed for parsing.
   */
  private static class ZKPatterns {

    // Delineates each File Source
    public static final Pattern SOURCE_SEPARATOR = Pattern.compile("___*");
    // Is this a link to a video file?
    public static final Predicate<Element> isVideoLink =
        elem -> {
          try {
            URL url = new URL(elem.attr("href"));
            // todo: do not hardcode ICDManager
            final Matcher urlMatcher = ICDData.getUrlMatcher().matcher(url.toString());
            // Perform link match test
            return ("a".equals(elem.tagName()) && urlMatcher.find());
          } catch (MalformedURLException ignored) {
          }

          return false;
        };
    // Selects the container elements for Event metadata
    public static final String METADATA_SELECTOR = "span span[style=color: green;]";
    // Is this a metadata container?
    public static final Predicate<Element> isMetadata =
        elem -> "span".equals(elem.tagName()) && !(elem.select(METADATA_SELECTOR).isEmpty());
  }
}
