package self.me.matchday.plugin.datasource.galataman;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.datasource.InvalidMetadataException;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventFileSource.Resolution;
import self.me.matchday.util.Log;

/**
 * Builder class to parse and create an EventFileSource from a GalatamanHDF post.
 */
final class GManFileSourceMetadataParser {

  private static final String LOG_TAG = "GManEventFileSrc";

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
  private String mediaContainer;
  private Long bitrate;
  private String videoCodec;
  private int frameRate;
  private String audioCodec;
  private int audioChannels;
  private String duration;
  private String size;
  private Resolution resolution;

  @NotNull
  @Contract("_ -> new")
  public static EventFileSource createFileSource(@NotNull final String html) {
    final GManFileSourceMetadataParser parser = new GManFileSourceMetadataParser(html);
    return
        EventFileSource
            .builder()
            .channel(parser.channel)
            .source(parser.source)
            .languages(parser.languages)
            .mediaContainer(parser.mediaContainer)
            .bitrate(parser.bitrate)
            .videoCodec(parser.videoCodec)
            .frameRate(parser.frameRate)
            .audioCodec(parser.audioCodec)
            .audioChannels(parser.audioChannels)
            .approximateDuration(parser.duration)
            .approximateFileSize(parser.size)
            .resolution(parser.resolution)
            .eventFiles(new TreeSet<>())
            .build();
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
   *             particular source.
   * @return A List of key/value tuples, each representing a metadata item.
   */
  private List<MetadataTuple> parseDataItems(@NotNull String data) {
    return
        // Break apart stream into individual data items,
        // based on patterns defined in the GalatamanPattern class...
        Arrays.stream(data.split(GManPatterns.METADATA_ITEM_DELIMITER))
            .filter((item) -> !("".equals(item))) // ... eliminating any empty entries ...
            .map( // ... convert to a tuple ...
                (String item) -> new MetadataTuple(item, GManPatterns.METADATA_KV_DELIMITER))
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
        parseVideoMetadata(value);
        break;
      case AUDIO:
        parseAudioMetadata(value);
        break;
      case DURATION:
        this.duration = value;
        break;
      case SIZE:
        this.size = value.replace("~", "");
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
   *                GalatamanPost class.
   */
  @NotNull
  private List<String> parseLanguages(@NotNull String langStr) {
    // Split string based on delimiter
    List<String> languages = new ArrayList<>(
        Arrays.asList(langStr.split(GManPatterns.LANGUAGE_DELIMITER)));
    // Remove empty entries
    languages.removeIf((lang) -> "".equals(lang.trim()));
    return languages;
  }

  /**
   * Split video data string apart.
   *
   * @param videoMetadata A String containing audio/video data items, separated by a delimiter
   *                      configured in the GalatamanPost class.
   */
  private void parseVideoMetadata(@NotNull String videoMetadata) {

    final long BITRATE_CONVERSION_FACTOR = 1_000_000L;
    // Video patterns
    final Pattern bitratePattern = Pattern.compile("[\\d.]+\\smbps", Pattern.CASE_INSENSITIVE);
    final Pattern containerPattern = Pattern.compile("\\w\\.\\d+ \\w+");
    final Pattern frameRatePattern = Pattern.compile("(\\d+)(fps)", Pattern.CASE_INSENSITIVE);

    // Split the string into data items & parse
    for (String dataItem : videoMetadata.split(GManPatterns.AV_DATA_DELIMITER)) {
      // Clean up data
      dataItem = dataItem.trim();
      if (bitratePattern.matcher(dataItem).find()) {
        final Matcher matcher = Pattern.compile("(\\d+)").matcher(dataItem);
        if (matcher.find()) {
          final String bitrate = matcher.group(1);
          this.bitrate = Long.parseLong(bitrate) * BITRATE_CONVERSION_FACTOR;
        }
      } else if (containerPattern.matcher(dataItem).find()) {
        // [video codec] [container]; ex: H.264 mkv
        final String[] containerParts = dataItem.split(" ");
        this.videoCodec = containerParts[0];
        this.mediaContainer = containerParts[1].toUpperCase();
      } else {
        final Matcher matcher = frameRatePattern.matcher(dataItem);
        if (matcher.find()) {
          // Get digit
          this.frameRate = Integer.parseInt(matcher.group(1));
        }
      }
    }
  }

  private void parseAudioMetadata(@NotNull final String audioData) {

    final Pattern channelPattern = Pattern.compile("([.\\d]+) (channels)");
    final Pattern bitratePattern = Pattern.compile("(\\d+) (kbps)", Pattern.CASE_INSENSITIVE);

    try {
      for (String dataItem : audioData.split(GManPatterns.AV_DATA_DELIMITER)) {

        dataItem = dataItem.trim();
        // Parse channel data
        if ("stereo".equals(dataItem)) {
          this.audioChannels = 2;
        } else if (channelPattern.matcher(dataItem).find()) {
          // Get numerical component of channel data
          final String[] channels = dataItem.split("channels");
          // Split main & sub-woofer channels
          final String[] split = channels[0].split("\\.");
          for (String channel : split) {
            // combine channels
            this.audioChannels += Integer.parseInt(channel.trim());
          }
        } else if (!(bitratePattern.matcher(dataItem).find())) {
          // Only other possibility is audio codec
          this.audioCodec = dataItem.toUpperCase();
        }
      }
    } catch (RuntimeException e) {
      Log.d(LOG_TAG, "Error parsing audio metadata from String: " + audioData, e);
    }
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

  /**
   * A class representing a key/value pair for a metadata item.
   */
  private static class MetadataTuple {

    private final String key;
    private final String value;

    public MetadataTuple(@NotNull String data, @NotNull String delimiter) {
      // Split into (hopefully) key/value pairs
      String[] kvPair = data.split(delimiter);

      // Ensure we have a tuple
      if (kvPair.length == 2) {
        this.key = kvPair[0];
        this.value = kvPair[1];
      } else {
        throw new InvalidMetadataException(
            "Could not split " + data + " with splitter " + delimiter);
      }
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
      if (o == this) {
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
