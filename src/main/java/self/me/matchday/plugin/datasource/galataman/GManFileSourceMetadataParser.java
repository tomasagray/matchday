/*
 * Copyright (c) 2020.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.plugin.datasource.galataman;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventFileSource.Resolution;
import self.me.matchday.model.FileSize;
import self.me.matchday.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/** Builder class to parse and create an EventFileSource from a GalatamanHDF post. */
@Component
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

  private final GManPatterns gManPatterns;

  public GManFileSourceMetadataParser(@Autowired final GManPatterns gManPatterns) {
    this.gManPatterns = gManPatterns;
  }

  @NotNull
  @Contract("_ -> new")
  public EventFileSource createFileSource(@NotNull final String html) {

    final GManFileMetadata metadata = new GManFileMetadata(html, gManPatterns);
    return EventFileSource.builder()
            .channel(metadata.channel)
            .source(metadata.source)
            .languages(metadata.languages)
            .mediaContainer(metadata.mediaContainer)
            .bitrate(metadata.bitrate)
            .videoCodec(metadata.videoCodec)
            .frameRate(metadata.frameRate)
            .audioCodec(metadata.audioCodec)
            .audioChannels(metadata.audioChannels)
            .approximateDuration(metadata.duration)
            .fileSize(metadata.fileSize)
            .resolution(metadata.resolution)
            .build();
  }

  private static class GManFileMetadata {

    // fields
    private final String metadataStr;
    private String languages;
    private String channel;
    private String source;
    private String mediaContainer;
    private Long bitrate;
    private String videoCodec;
    private int frameRate;
    private String audioCodec;
    private int audioChannels;
    private String duration;
    private Long fileSize;
    private Resolution resolution;
    // dependencies
    private final GManPatterns gManPatterns;

    // Constructor
    private GManFileMetadata(@NotNull final String matchDataHTML, @NotNull final GManPatterns gManPatterns) {

      // Get patterns instance
      this.gManPatterns = gManPatterns;

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
     *     particular source.
     * @return A List of key/value tuples, each representing a metadata item.
     */
    private List<MetadataTuple> parseDataItems(@NotNull String data) {
      return
      // Break apart stream into individual data items,
      // based on patterns defined in the GalatamanPattern class...
      Arrays.stream(data.split(gManPatterns.getMetadataDelimiter()))
          // ... eliminating any empty entries ...
          .filter((item) -> !("".equals(item)))
          // ... convert to a tuple ...
          .map((String item) -> new MetadataTuple(item, gManPatterns.getMetadataKvDelimiter()))
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
          this.languages = parseLanguages(value);
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
          this.fileSize = parseFileSize(value);
          break;
        case RESOLUTION:
          this.resolution = parseResolution(value);
          break;
        default:
          throw new IllegalArgumentException(
              "Invalid key/value in Galataman Event File Source metadata: " + kv.toString());
      }
    }

    /**
     * Break apart a String into language names.
     *
     * @param langStr A String containing language names, separated by a delimiter configured in the
     *     GalatamanPost class.
     */
    private @NotNull String parseLanguages(@NotNull String langStr) {
      // Split string based on delimiter
      List<String> languages =
          new ArrayList<>(Arrays.asList(langStr.split(gManPatterns.getLanguageDelimiter())));
      // Remove empty entries
      languages.removeIf((lang) -> "".equals(lang.trim()));
      return String.join(", ", languages);
    }

    /**
     * Split video data string apart.
     *
     * @param videoMetadata A String containing audio/video data items, separated by a delimiter
     *     configured in the GalatamanPost class.
     */
    private void parseVideoMetadata(@NotNull String videoMetadata) {

      // Split the string into data items & parse
      for (String dataItem : videoMetadata.split(gManPatterns.getAvDataDelimiter())) {
        // Clean up data
        dataItem = dataItem.trim();

        // Test for bitrate data
        final Matcher bitrateMatcher = gManPatterns.getBitrateMatcher(dataItem);
        if (bitrateMatcher.find()) {
          final String bitrate = bitrateMatcher.group(1);
          this.bitrate = Long.parseLong(bitrate) * gManPatterns.getBitrateConversionFactor();

        } else if (gManPatterns.getContainerMatcher(dataItem).find()) {
          // [video codec] [container]; ex: H.264 mkv
          final String[] containerParts = dataItem.split(" ");
          this.videoCodec = containerParts[0];
          this.mediaContainer = containerParts[1].toUpperCase();

        } else {
          final Matcher matcher = gManPatterns.getFramerateMatcher(dataItem);
          if (matcher.find()) {
            // Get digit
            this.frameRate = Integer.parseInt(matcher.group(1));
          }
        }
      }
    }

    /**
     * Parses the audio metadata from a String
     *
     * @param audioData The String containing audio data
     */
    private void parseAudioMetadata(@NotNull final String audioData) {

      try {
        for (String dataItem : audioData.split(gManPatterns.getAvDataDelimiter())) {

          dataItem = dataItem.trim();
          // Parse channel data
          if ("stereo".equals(dataItem)) {
            this.audioChannels = 2;
          } else if (gManPatterns.getChannelMatcher(dataItem).find()) {
            // Get numerical component of channel data
            final String[] channels = dataItem.split("channels");
            // Split main & sub-woofer channels
            final String[] split = channels[0].split("\\.");
            for (String channel : split) {
              // combine channels
              this.audioChannels += Integer.parseInt(channel.trim());
            }
          } else if (!(gManPatterns.getBitrateMatcher(dataItem).find())) {
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
     * Extract approximate file size data
     *
     * @param data The String containing the data
     * @return The approximate file size of the video source
     */
    private Long parseFileSize(@NotNull final String data) {

      Long result = null;

      // Americanize
      final String decimalData = data.replace(",", ".");

      final Matcher matcher = gManPatterns.getFilesizeMatcher(decimalData);
      if (matcher.find()) {
        final float size = Float.parseFloat(matcher.group(1));
        final String units = matcher.group(2).toUpperCase();
        // Create FileSize object
        switch (units) {
          case "GB":
            result = FileSize.ofGigabytes(size);
            break;
          case "MB":
            result = FileSize.ofMegabytes(size);
            break;
          case "KB":
            result = FileSize.ofKilobytes(size);
            break;
        }
      }
      return result;
    }

    /**
     * Removes any remaining tags and special characters e.g., <br>
     * , &nbsp;, etc.
     *
     * @param input A String in need of cleaning.
     * @return The cleaned String.
     */
    private @NotNull String clean(@NotNull String input) {
      return input.replaceAll("<[^>]*>", "").trim();
    }
  }

  /** A class representing a key/value pair for a metadata item. */
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
        throw new IllegalArgumentException(
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
