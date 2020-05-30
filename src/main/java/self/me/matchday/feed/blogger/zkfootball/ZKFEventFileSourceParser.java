package self.me.matchday.feed.blogger.zkfootball;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import self.me.matchday.feed.blogger.InvalidBloggerPostException;
import self.me.matchday.fileserver.inclouddrive.ICDData;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFile.EventPartIdentifier;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventFileSource.Resolution;
import self.me.matchday.feed.IEventFileSourceParser;
import self.me.matchday.util.Log;

/**
 * Implementation of the Event File Source parser, specific to the ZKFootball blog, found at:
 * https://zkfootballmatch.blogspot.com
 */
public class ZKFEventFileSourceParser implements IEventFileSourceParser {

  private static final String LOG_TAG = "ZKEventFileSrcParser";

  // Default values
  private static final Long DEFAULT_BITRATE = 4_000_000L;

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
        List<EventFile> eventFiles = new ArrayList<>();

        // find next source - links (w/titles), metadata
        while (token != null && !(ZKPatterns.SOURCE_SEPARATOR.matcher(token.text()).find())) {

          // find part title
          if (EventPartIdentifier.isPartIdentifier(token.html())) {
            eventPartIdentifier = EventPartIdentifier.fromString(token.text());
          } else if (ZKPatterns.isVideoLink.test(token)) {
            // Create EventFile from link w/ above title
            try {
              final URL url = new URL(token.attr("href"));
              EventFile eventFile = new EventFile(eventPartIdentifier, url);
              // add to local collection
              eventFiles.add(eventFile);
            } catch (MalformedURLException e) {
              Log.d(LOG_TAG, "Could not parse href: " + token.attr("href"));
            }
          } else if (ZKPatterns.isMetadata.test(token)) {
            // Populate EventFileSource metadata
            final Elements elements = token.select(ZKPatterns.METADATA_SELECTOR);
            final EventFileSource eventFileSource = ZKFMetadataParser.fromElements(elements);
            eventFileSource.getEventFiles().addAll(eventFiles);
            // add complete file source to list
            eventFileSources.add(eventFileSource);
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

  private static class ZKFEventFileSource extends EventFileSource {

    ZKFEventFileSource(@NotNull ZKFMetadataParser metadata) {

      // Unpack builder object
      setChannel(metadata.channel);
      setApproximateFileSize(metadata.approxFileSize);
      setResolution(metadata.resolution);
      setMediaContainer(metadata.mediaContainer);
      setBitrate(metadata.bitrate);
      setFrameRate(metadata.frameRate);
      setLanguages(Collections.unmodifiableList(metadata.languages));
    }
  }

  private static class ZKFMetadataParser {

    // Metadata tags
    private static final String CHANNEL = "channel:";
    private static final String LANGUAGE = "language:";
    private static final String FORMAT = "format:";
    private static final String BITRATE = "bitrate:";
    private static final String SIZE = "size:";


    private String channel;
    private final List<String> languages = new ArrayList<>();
    private Resolution resolution;
    private int frameRate;
    private String mediaContainer;
    private long bitrate;
    private String approxFileSize;

    private ZKFMetadataParser(@NotNull final Elements elements) {
      parseEventMetadata(elements);
    }

    @Contract("_ -> new")
    public static @NotNull ZKFEventFileSource fromElements(@NotNull final Elements elements) {
      final ZKFMetadataParser zkfMetadataParser = new ZKFMetadataParser(elements);
      return new ZKFEventFileSource(zkfMetadataParser);
    }

    /**
     * Parse a collection of elements into metadata for this Event
     *
     * @param elements A Collection of Jsoup elements
     */
    private void parseEventMetadata(@NotNull final Elements elements) {

      elements.forEach(element -> {
        // analyze metadata tag
        switch (element.text()) {
          case CHANNEL:
            this.channel = cleanMetadata(element.nextSibling().toString());
            break;

          case LANGUAGE:
            // get language from next span
            final String langString = element.nextElementSibling().select("b").text();
            parseLanguages(langString);
            break;

          case FORMAT:
            // Get format String
            final Node sibling = element.nextSibling();
            parseFormat(sibling.toString());
            break;

          case BITRATE:
            // Parse bitrate
            final Node bitrateNode = element.nextSibling();
            final String bitrate = cleanMetadata(bitrateNode.toString()).trim();
            parseBitrate(bitrate);
            break;

          case SIZE:
            // Set file size from final element
            this.approxFileSize = cleanMetadata(element.nextSibling().toString());
            break;

          default:
            throw new InvalidBloggerPostException("Could not parse metadata item: " + element);
        }
      });
    }

    /**
     * Parse the video bitrate into a Long
     *
     * @param bitrate A String containing bitrate data
     */
    private void parseBitrate(String bitrate) {

      try {
        // Split digit from unit
        final Matcher bitrateMatcher = Pattern.compile("(\\d+)").matcher(bitrate);
        if (bitrateMatcher.find()) {
          final int digit = Integer.parseInt(bitrateMatcher.group());
          // Parse conversion factor
          if (ZKPatterns.mbpsPattern.matcher(bitrate).find()) {
            this.bitrate = (digit * 1_000_000L);
          } else if (ZKPatterns.kbpsPattern.matcher(bitrate).find()) {
            this.bitrate = (digit * 1_000L);
          }
        }
      } catch (NumberFormatException e) {
        Log.d(LOG_TAG, "Error parsing bitrate data", e);
      } finally {
        // Ensure bitrate has been set
        if (this.bitrate == 0) {
          Log.d(LOG_TAG, String.format(
              "Could not parse EventFileSource bitrate from String: %s; defaulting to 4MBps",
              bitrate));
          this.bitrate = DEFAULT_BITRATE;
        }
      }
    }

    /**
     * Parses format data, including video resolution, frame rate & media container
     *
     * @param data An array of Strings representing the various metadata items
     */
    private void parseFormat(@NotNull final String data) {

      // Split into component parts
      final String[] format = data.split(" ");

      try {
        for (String part : format) {
          // Special 4k handler
          if (part.contains("4096x2160")) {
            this.resolution = Resolution.R_4k;
            // 720 & 1080
          } else if (ZKPatterns.resolutionPattern.matcher(part).find()) {
            this.resolution = Resolution.fromString(part);
          } else {
            final Matcher frMatcher = ZKPatterns.frameRatePattern.matcher(part);
            if (frMatcher.find()) {
              // Parse frame rate
              this.frameRate = Integer.parseInt(frMatcher.group(1));
            } else if (ZKPatterns.containerPattern.matcher(part).find()) {
              this.mediaContainer = part.toUpperCase();
            }
          }
        }
      } catch (RuntimeException e) {
        Log.d(LOG_TAG, String.format("Could not parse format data from supplied String: %s", data),
            e);
      }
    }

    /**
     * Parses a given String into languages & adds them to the list
     *
     * @param langString The String containing language data
     */
    private void parseLanguages(String langString) {
      final String language = cleanMetadata(langString);
      // capitalize first letter
      String Language = language.substring(0, 1).toUpperCase() + language.substring(1);
      this.languages.add(Language);
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
            final Matcher urlMatcher = ICDData.getUrlMatcher(url.toString());
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

    // Format patterns
    public static final Pattern resolutionPattern = Pattern
        .compile("(720|1080)[pi]", Pattern.CASE_INSENSITIVE);
    public static final Pattern frameRatePattern = Pattern
        .compile("(\\d+)(fps)", Pattern.CASE_INSENSITIVE);
    public static final Pattern containerPattern = Pattern
        .compile("mkv|ts", Pattern.CASE_INSENSITIVE);

    // Bitrate patterns
    public static final Pattern mbpsPattern = Pattern.compile("mb/sec", Pattern.CASE_INSENSITIVE);
    public static final Pattern kbpsPattern = Pattern.compile("kbps", Pattern.CASE_INSENSITIVE);


  }
}
