/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.blogger.galataman;

import static self.me.matchday.feed.blogger.galataman.GManEventFileSourceParser.GalatamanPattern.AV_DATA_DELIMITER;
import static self.me.matchday.feed.blogger.galataman.GManEventFileSourceParser.GalatamanPattern.LANGUAGE_DELIMITER;
import static self.me.matchday.feed.blogger.galataman.GManEventFileSourceParser.GalatamanPattern.METADATA_ITEM_DELIMITER;
import static self.me.matchday.feed.blogger.galataman.GManEventFileSourceParser.GalatamanPattern.METADATA_KV_DELIMITER;
import static self.me.matchday.feed.blogger.galataman.GManEventFileSourceParser.GalatamanPattern.START_OF_SOURCE;
import static self.me.matchday.feed.blogger.galataman.GManEventFileSourceParser.GalatamanPattern.isSourceData;
import static self.me.matchday.feed.blogger.galataman.GManEventFileSourceParser.GalatamanPattern.isVideoLink;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import self.me.matchday.feed.IEventFileSourceParser;
import self.me.matchday.feed.InvalidMetadataException;
import self.me.matchday.fileserver.inclouddrive.ICDData;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFile.EventPartIdentifier;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventFileSource.Resolution;
import self.me.matchday.util.Log;

/**
 * Represents a specific source for an Event (for example: 1080i, Spanish), derived from the
 * Galataman blog (http://galatamanhdf.blogspot.com/)
 *
 * @author tomas
 */
public final class GManEventFileSourceParser implements IEventFileSourceParser {

  private static final String LOG_TAG = "GManMatchSource";

  private final String html;
  private final List<EventFileSource> eventFileSources;

  public GManEventFileSourceParser(@NotNull final String html) {
    this.html = html;
    this.eventFileSources = new ArrayList<>();
    parseMatchSources();
  }

  @Override
  public List<EventFileSource> getEventFileSources() {
    return eventFileSources;
  }

  /**
   * Extracts match source data from this post.
   */
  private void parseMatchSources() {

    try {
      // DOM-ify HTML content for easy manipulation
      Document doc = Jsoup.parse(this.html);

      // Since this is a loosely structured document, we will use a token, starting at the first
      // source and looking for what we want along the way
      Element token = doc.getElementsMatchingOwnText(START_OF_SOURCE).first();

      // Search until the end of the Document
      while (token != null) {
        // When we find a source
        if (isSourceData.test(token)) {
          // Save HTML
          String metadata = token.html();
          // Video files for this source
          List<EventFile> eventFiles = new ArrayList<>();

          // Now, continue searching, this time for links,
          // until the next source or the end of the HTML
          Element innerToken = token.nextElementSibling();
          EventPartIdentifier partIdentifier = EventPartIdentifier.DEFAULT;

          while ((innerToken != null) && !(isSourceData.test(innerToken))) {
            // Look for a part identifier
            final String tokenHtml = innerToken.html();
            if (EventPartIdentifier.isPartIdentifier(tokenHtml)) {
              // Create an identifier for this part
              partIdentifier = EventPartIdentifier.fromString(tokenHtml);
            } else if (isVideoLink.test(innerToken)) {
              // When we find a link to a video file, extract href attribute & add it to our
              // source's list of EventFiles, with an identifier (might be null)
              final URL url = new URL(innerToken.attr("href"));
              eventFiles.add(
                  new EventFile(partIdentifier, url)
              );
            }

            // Advance inner token
            innerToken = innerToken.nextElementSibling();
          }

          final GManEventFileSource eventFileSource =
              new GManEventFileSource(GManFileSourceMetadataParser.fromHTML(metadata), eventFiles);

          // Add match source to object
          this.eventFileSources.add(eventFileSource);
        }

        // Advance the search token
        token = token.nextElementSibling();
      }

    } catch (MalformedURLException e) {
      // Log the error
      Log.e(LOG_TAG, "There was a problem parsing a Galataman post", e);
    }
  }

  private static class GManEventFileSource extends EventFileSource {

    // Constructor
    GManEventFileSource(@NotNull final GManFileSourceMetadataParser metadata,
        @NotNull final List<EventFile> eventFiles) {
      // Unpack builder object
      setChannel(metadata.channel);
      setSource(metadata.source);
      setApproximateDuration(metadata.duration);
      setApproximateFileSize(metadata.size);
      setResolution(metadata.resolution);

      // Initialize immutable List fields
      setLanguages(Collections.unmodifiableList(metadata.languages));
      setVideoData(Collections.unmodifiableList(metadata.videoData));
      setAudioData(Collections.unmodifiableList(metadata.audioData));
      setEventFiles(Collections.unmodifiableList(eventFiles));
    }
  }

  /**
   * Builder class to parse and create an EventSource from a GalatamanHDF post.
   */
  private static final class GManFileSourceMetadataParser {

    // Metadata identifiers
    private static final String CHANNEL = "CHANNEL";
    private static final String SOURCE = "SOURCE";
    private static final String LANGUAGE = "LANGUAGE";
    private static final String COMMENTARY = "COMMENTARY";
    private static final String VIDEO = "VIDEO";
    private static final String AUDIO = "AUDIO";
    private static final String DURATION = "DURATION";
    private static final String SIZE = "SIZE";
    private static final String RESOLUTION = "RELEASE";

    // Fields
    private final String metadataStr;
    private String channel;
    private String source;
    private final List<String> languages = new ArrayList<>();
    private final List<String> videoData = new ArrayList<>();
    private final List<String> audioData = new ArrayList<>();
    private String duration;
    private String size;
    private Resolution resolution;

    @NotNull
    @Contract("_ -> new")
    public static GManFileSourceMetadataParser fromHTML(@NotNull final String html) {
      return new GManFileSourceMetadataParser(html);
    }

    // Constructor
    private GManFileSourceMetadataParser(@NotNull String matchDataHTML) {
      // Save raw metadata
      this.metadataStr = matchDataHTML;
      // Cleanup HTML, removing superfluous &nbsp; and parse data into items
      parseDataItems(matchDataHTML.replace("&nbsp;", ""))
          // Parse each data item
          .forEach(this::parseDataItem);
    }

    /**
     * Break the Event data string apart into parse-able chunks, and collect them into a List.
     *
     * @param data A String containing raw HTML to be parsed into metadata items concerning a
     * particular source.
     * @return A List of key/value tuples, each representing a metadata item.
     */
    private List<MetadataTuple> parseDataItems(@NotNull String data) {
      return
          // Break apart stream into individual data items,
          // based on patterns defined in the GalatamanPattern class...
          Arrays.stream(data.split(METADATA_ITEM_DELIMITER))
              .filter((item) -> !("".equals(item))) // ... eliminating any empty entries ...
              .map( // ... convert to a tuple ...
                  (String item) -> new MetadataTuple(item, METADATA_KV_DELIMITER))
              // ... finally, collect to a List and return
              .collect(Collectors.toList());
    }

    /**
     * Examines a key/value pair to determine if they relate to a relevant metadata item (e.g.,
     * Channel, Source, etc.)
     *
     * @param kv A key/value pair containing data about this source.
     */
    private void parseDataItem(@NotNull MetadataTuple kv) {
      // Get the key
      String key = kv.getKeyString();
      // Clean up the value
      String value = clean(kv.getValueString());

      // Examine the key and assign the value to the correct metadata field
      switch (key) {
        case CHANNEL:
          this.channel = value;
          break;
        case SOURCE:
          this.source = value;
          break;
        case LANGUAGE:
        case COMMENTARY:
          this.languages.addAll(parseLanguages(value));
          break;
        case VIDEO:
          this.videoData.addAll(parseAVData(value));
          break;
        case AUDIO:
          this.audioData.addAll(parseAVData(value));
          break;
        case DURATION:
          this.duration = value;
          break;
        case SIZE:
          this.size = value;
          break;
        case RESOLUTION:
          this.resolution = parseResolution(value);
          break;
        default:
          throw new InvalidMetadataException(
              "Invalid key/value in Galataman Event File Source metadata: " + kv.toString());
      }
    }

    /**
     * Break apart a String into language names.
     *
     * @param langStr A String containing language names, separated by a delimiter configured in the
     * GalatamanPost class.
     */
    @NotNull
    private List<String> parseLanguages(@NotNull String langStr) {
      // Split string based on delimiter
      List<String> languages = new ArrayList<>(Arrays.asList(langStr.split(LANGUAGE_DELIMITER)));
      // Remove empty entries
      languages.removeIf((lang) -> "".equals(lang.trim()));

      return languages;
    }

    /**
     * Split video data string apart.
     *
     * @param avData A String containing audio/video data items, separated by a delimiter configured
     * in the GalatamanPost class.
     */
    @NotNull
    private List<String> parseAVData(@NotNull String avData) {
      // Temporary container
      List<String> dataItems = new ArrayList<>();

      // Split the string into data items
      for (String dataItem : avData.split(AV_DATA_DELIMITER)) {
        // Remove empty space
        String trim = dataItem.trim();
        if (!("".equals(trim)))
        // Add data to object
        {
          dataItems.add(trim);
        }
      }

      return dataItems;
    }

    /**
     * Determine the appropriate enumerated video resolution for this video source.
     *
     * @param resolution The String representing the video resolution
     * @return An enumerated video resolution value.
     */
    private Resolution parseResolution(@NotNull String resolution) {
      // Analyze resolution & return
      if (Resolution.isResolution(resolution)) {
        return Resolution.fromString(resolution);
      } else {
        Log.i(
            LOG_TAG,
            "Could not determine video resolution for file source: "
                + metadataStr
                + "; defaulting to SD");
        return Resolution.R_SD;
      }
    }

    /**
     * Removes any remaining tags and special characters e.g., <br> , &nbsp;, etc.
     *
     * @param input A String in need of cleaning.
     * @return The cleaned String.
     */
    @NotNull
    private String clean(@NotNull String input) {
      return input.replaceAll("<[^>]*>", "").trim();
    }

    /** A class representing a key/value pair for a metadata item. */
    public static class MetadataTuple {
      private final String key;
      private final String value;

      public MetadataTuple(@NotNull String data, @NotNull String delimiter) {
        // Split into (hopefully) key/value pairs
        String[] kvPair = data.split(delimiter);

        // Ensure we have a tuple
        if (kvPair.length == 2) {
          this.key = kvPair[0];
          this.value = kvPair[1];
        } else
          throw new InvalidMetadataException("Could not split " + data + " with splitter " + delimiter);
      }

      /**
       * Returns the tuple key as an uppercase String.
       *
       * @return String The key of the tuple.
       */
      public String getKeyString() {
        return this.key.toUpperCase();
      }

      /**
       * Returns the tuple value
       *
       * @return String The value of the tuple.
       */
      public String getValueString() {
        return this.value;
      }

      @Override
      public boolean equals(Object o) {
        if(o == this) {
          return true;
        }
        if (!(o instanceof MetadataTuple)) {
          return false;
        }

        MetadataTuple kv = (MetadataTuple) o;

        return this.key.equals(kv.key) && this.value.equals(kv.value);
      }

      @Override
      public int hashCode() {
        int hash = 5;
        hash = (19 * hash) + Objects.hashCode(this.key);
        hash = (19 * hash) + Objects.hashCode(this.value);
        return hash;
      }

      @Override
      public String toString() {
        return "Key: " + this.key + ", Value: " + this.value;
      }
    }
  }

  /**
   * Collection class of Patterns specific to the Galataman HDF Blog.
   */
  static class GalatamanPattern {

    // Entry parsing patterns
    static final String START_OF_SOURCE = Pattern.compile("__*").pattern();

    static final String METADATA_ITEM_DELIMITER =
        Pattern.compile("<span style=\"color: blue;\">(\\[)?").pattern();

    static final String METADATA_KV_DELIMITER =
        Pattern.compile("(])?</span>:(<span [^>]*>)?").pattern();

    static final String LANGUAGE_DELIMITER = Pattern.compile("[\\d.* ]|/").pattern();

    static final String AV_DATA_DELIMITER = Pattern.compile("‖").pattern();


    // Predicates
    static final Predicate<Element> isSourceData =
        elem -> ("b".equals(elem.tagName())) && (elem.text().contains("Channel"));

    static final Predicate<Element> isVideoLink =
        elem ->
            ("a".equals(elem.tagName()))
                && (ICDData.getUrlMatcher().matcher(elem.attr("href")).find());

  }
}
