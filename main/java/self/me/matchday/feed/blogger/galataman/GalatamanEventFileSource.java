/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.blogger.galataman;

import static self.me.matchday.feed.blogger.galataman.GalatamanPattern.AV_DATA_DELIMITER;
import static self.me.matchday.feed.blogger.galataman.GalatamanPattern.LANGUAGE_DELIMITER;
import static self.me.matchday.feed.blogger.galataman.GalatamanPattern.METADATA_ITEM_DELIMITER;
import static self.me.matchday.feed.blogger.galataman.GalatamanPattern.METADATA_KV_DELIMITER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.InvalidMetadataException;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.MetadataTuple;
import self.me.matchday.util.Log;

/**
 * Represents a specific source for an Event (for example: 1080i, Spanish), derived from the
 * Galataman blog (http://galatamanhdf.blogspot.com/)
 *
 * @author tomas
 */
@Entity
@NoArgsConstructor
public final class GalatamanEventFileSource extends EventFileSource {

  private static final String LOG_TAG = "GManMatchSource";

  @Override
  public String getChannel() {
    return this.channel;
  }

  @Override
  public String getSource() {
    return this.source;
  }

  @Override
  public List<String> getLanguages() {
    return this.languages;
  }

  @Override
  public List<String> getVideoData() {
    return this.videoData;
  }

  @Override
  public List<String> getAudioData() {
    return this.audioData;
  }

  @Override
  public String getApproximateDuration() {
    return this.approximateDuration;
  }

  @Override
  public String getApproximateFileSize() {
    return this.approximateFileSize;
  }

  @Override
  public Resolution getResolution() {
    return this.resolution;
  }

  @Override
  public List<EventFile> getEventFiles() {
    return this.eventFiles;
  }

  // Constructor
  private GalatamanEventFileSource(@NotNull GalatamanEventFileSource.GalatamanEventFileSourceBuilder builder) {
    // Unpack builder object
    this.channel = builder.channel;
    this.source = builder.source;
    this.approximateDuration = builder.duration;
    this.approximateFileSize = builder.size;
    this.resolution = builder.resolution;

    // Initialize immutable List fields
    this.languages = Collections.unmodifiableList(builder.languages);
    this.videoData = Collections.unmodifiableList(builder.videoData);
    this.audioData = Collections.unmodifiableList(builder.audioData);
    this.eventFiles = Collections.unmodifiableList(builder.eventFiles);
  }

  /** Builder class to parse and create an EventSource from a GalatamanHDF post. */
  static final class GalatamanEventFileSourceBuilder {
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
    private final List<EventFile> eventFiles;

    // Constructor
    GalatamanEventFileSourceBuilder(
        @NotNull String matchDataHTML, final List<EventFile> eventFiles) {
      // Save raw metadata
      this.metadataStr = matchDataHTML;
      this.eventFiles = eventFiles;

      // Cleanup HTML, removing superfluous &nbsp; and parse data into items
      parseDataItems(matchDataHTML.replace("&nbsp;", ""))
          // Parse each data item
          .forEach(this::parseDataItem);
    }

    /**
     * Break the Event data string apart into parse-able chunks, and collect them into a List.
     *
     * @param data A String containing raw HTML to be parsed into metadata items concerning a
     *     particular source.
     * @return A List of key/value tuples, each representing a metadata item
     */
    private List<MetadataTuple> parseDataItems(@NotNull String data) {
      return
      // Break apart stream into individual data items,
      // based on patterns defined in the GalatamanPattern class...
      Arrays.stream(data.split(METADATA_ITEM_DELIMITER))
          .filter((item) -> !("".equals(item))) // ... eliminating any empty entries ...
          .map( // ... convert to a tuple ...
              (String item) -> {
                try {
                  return new MetadataTuple(item, METADATA_KV_DELIMITER);
                } catch (InvalidMetadataException e) {
                  Log.d(LOG_TAG, "Invalid metadata: " + e.getMessage());
                  throw new GalatamanPostParseException(
                      "Galataman data corrupted:\n" + this.metadataStr, e);
                }
              })
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

      // Examine the key and assign the value to the correct
      // metadata field
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
          throw new GalatamanPostParseException("INVALID KEY/VALUE: " + kv.toString());
      }
    }

    /**
     * Break apart a String into language names.
     *
     * @param langStr A String containing language names, separated by a delimiter configured in the
     *     GalatamanPost class.
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
     *     in the GalatamanPost class.
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
          dataItems.add(trim);
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
     * Removes any remaining tags and special characters e.g., <br>
     * , &nbsp;, etc.
     *
     * @param input A String in need of cleaning.
     * @return The cleaned String.
     */
    @NotNull
    private String clean(@NotNull String input) {
      return input.replaceAll("<[^>]*>", "").trim();
    }

    @NotNull
    @Contract(" -> new")
    GalatamanEventFileSource build() {
      return new GalatamanEventFileSource(this);
    }
  }
}
