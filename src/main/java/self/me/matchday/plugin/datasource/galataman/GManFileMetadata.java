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

import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.EventFileSource.Resolution;
import self.me.matchday.model.FileSize;
import self.me.matchday.model.FileSourceMetadata;
import self.me.matchday.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

class GManFileMetadata extends FileSourceMetadata {

    private static final String LOG_TAG = "GManFileMetadata";

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
    
    // fields
    private final String metadataStr;
    // dependencies
    private final GManPatterns gManPatterns;

    // Constructor
    GManFileMetadata(@NotNull final String html, @NotNull final GManPatterns gManPatterns) {

        // Get patterns instance
        this.gManPatterns = gManPatterns;

        // Save raw metadata
        this.metadataStr = html;
        // Cleanup HTML, removing superfluous &nbsp; and parse data into items
        parseDataItems(html.replace("&nbsp;", ""))
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
                setChannel(value);
                break;
            case SOURCE:
                setSource(value);
                break;
            case LANGUAGE:
            case COMMENTARY:
                final String languages = parseLanguages(value);
                setLanguages(languages);
                break;
            case VIDEO:
                parseVideoMetadata(value);
                break;
            case AUDIO:
                parseAudioMetadata(value);
                break;
            case DURATION:
                setApproximateDuration(value);
                break;
            case SIZE:
                final long fileSize = parseFileSize(value);
                setFileSize(fileSize);
                break;
            case RESOLUTION:
                final Resolution resolution = parseResolution(value);
                setResolution(resolution);
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
     *                GalatamanPost class.
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
     *                      configured in the GalatamanPost class.
     */
    private void parseVideoMetadata(@NotNull String videoMetadata) {

        // Split the string into data items & parse
        for (String dataItem : videoMetadata.split(gManPatterns.getAvDataDelimiter())) {
            // Clean up data
            dataItem = dataItem.trim();

            // Test for bitrate data
            final Matcher bitrateMatcher = gManPatterns.getBitrateMatcher(dataItem);
            if (bitrateMatcher.find()) {
                final String bitrateData = bitrateMatcher.group(1);
                final long bitrate = Long.parseLong(bitrateData) * gManPatterns.getBitrateConversionFactor();
                setBitrate(bitrate);

            } else if (gManPatterns.getContainerMatcher(dataItem).find()) {
                // [video codec] [container]; ex: H.264 mkv
                final String[] containerParts = dataItem.split(" ");
                final String videoCodec = containerParts[0];
                final String mediaContainer = containerParts[1].toUpperCase();
                setVideoCodec(videoCodec);
                setMediaContainer(mediaContainer);

            } else {
                final Matcher matcher = gManPatterns.getFramerateMatcher(dataItem);
                if (matcher.find()) {
                    // Get digit
                    final int frameRate = Integer.parseInt(matcher.group(1));
                    setFrameRate(frameRate);
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
                    final int audioChannels = 2;
                    setAudioChannels(audioChannels);

                } else if (gManPatterns.getChannelMatcher(dataItem).find()) {

                    // Get numerical component of channel data
                    final String[] channels = dataItem.split("channels");
                    // Split main & sub-woofer channels
                    final String[] split = channels[0].split("\\.");
                    int audioChannels = 0;
                    for (String channel : split) {
                        // combine channels
                       audioChannels += Integer.parseInt(channel.trim());
                    }
                    setAudioChannels(audioChannels);

                } else if (!(gManPatterns.getBitrateMatcher(dataItem).find())) {
                    // Only other possibility is audio codec
                    final String audioCodec = dataItem.toUpperCase();
                    setAudioCodec(audioCodec);
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
     * Extract approximate file size data from a String and return as a number
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
