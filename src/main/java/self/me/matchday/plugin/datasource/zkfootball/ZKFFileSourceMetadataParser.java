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

package self.me.matchday.plugin.datasource.zkfootball;

import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventFileSource.Resolution;
import self.me.matchday.model.FileSize;
import self.me.matchday.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ZKFFileSourceMetadataParser {

  private static final String LOG_TAG = "ZKFFileSourceMetadataParser";

  // Metadata tags
  private static final String CHANNEL = "channel:";
  private static final String LANGUAGE = "language:";
  private static final String FORMAT = "format:";
  private static final String BITRATE = "bitrate:";
  private static final String SIZE = "size:";

  private final ZKFPatterns zkfPatterns;

  public ZKFFileSourceMetadataParser(@Autowired final ZKFPatterns zkfPatterns) {
    this.zkfPatterns = zkfPatterns;
  }

  public EventFileSource createFileSource(@NotNull final Elements elements) {

    final ZKFFileMetadata metadata = new ZKFFileMetadata(elements, zkfPatterns);
    return EventFileSource.builder()
        .channel(metadata.channel)
        .languages(metadata.languages)
        .resolution(metadata.resolution)
        .frameRate(metadata.frameRate)
        .mediaContainer(metadata.mediaContainer)
        .bitrate(metadata.bitrate)
        .fileSize(metadata.fileSize)
        .build();
  }

  private static class ZKFFileMetadata {

    private String channel;
    private String languages;
    private Resolution resolution;
    private int frameRate;
    private String mediaContainer;
    private long bitrate;
    private Long fileSize;
    // dependencies
    private final ZKFPatterns zkfPatterns;

    private ZKFFileMetadata(
        @NotNull final Elements elements, @NotNull final ZKFPatterns zkfPatterns) {
      this.zkfPatterns = zkfPatterns;
      parseEventMetadata(elements);
    }

    /**
     * Parse a collection of elements into metadata for this Event
     *
     * @param elements A Collection of Jsoup elements
     */
    private void parseEventMetadata(@NotNull final Elements elements) {

      elements.forEach(
          element -> {
            // analyze metadata tag
            final String text = element.text();
            switch (text) {
              case CHANNEL:
                this.channel = parseChannel(element);
                break;

              case LANGUAGE:
                this.languages = parseLanguages(element);
                break;

              case FORMAT:
                // sets multiple fields
                parseFormat(element);
                break;

              case BITRATE:
                this.bitrate = parseBitrate(element);
                break;

              case SIZE:
                // Set file size from final element
                this.fileSize = parseFileSize(element);
                break;
            }
          });
    }

    /**
     * Extract TV channel data from HTML DOM
     *
     * @param element An HTML DOM collection
     * @return The channel this video source was recorded from
     */
    private @NotNull String parseChannel(Element element) {
      final Node sibling = element.nextSibling();
      return (sibling != null) ? cleanMetadata(sibling.toString()) : "";
    }

    /**
     * Parses a given String into languages & adds them to the list
     *
     * @param element The HTML containing language data
     * @return A collection of language Strings
     */
    private String parseLanguages(@NotNull final Element element) {

      // Result container
      final List<String> languages = new ArrayList<>();

      try {
        // get language from next span
        final Element sibling = element.nextElementSibling();
        final String langData = sibling.select("b").text();

        final String language = cleanMetadata(langData);
        // capitalize first letter
        String Language = language.substring(0, 1).toUpperCase() + language.substring(1);
        languages.add(Language);

      } catch (NullPointerException e) {
        Log.i(LOG_TAG, "Error parsing language data from Element: " + element, e);
      }

      return String.join(", ", languages);
    }

    /**
     * Parses format data, including video resolution, frame rate & media container
     *
     * @param element HTML representing the various metadata items
     */
    private void parseFormat(@NotNull final Element element) {

      // Get format String
      final String data = element.nextSibling().toString();

      // Split into component parts
      final String[] format = data.split(" ");

      try {
        for (String part : format) {

          // Resolution - 4k handler
          if (part.contains("4096x2160")) {
            this.resolution = Resolution.R_4k;
            // Other resolutions
          } else if (zkfPatterns.getResolutionMatcher(part).find()) {
            this.resolution = Resolution.fromString(part);
          } else {

            // Frame rate
            final Matcher frMatcher = zkfPatterns.getFramerateMatcher(part);
            if (frMatcher.find()) {
              // Parse frame rate
              this.frameRate = Integer.parseInt(frMatcher.group(1));

            } else if (zkfPatterns.getContainerMatcher(part).find()) {
              // Media container
              this.mediaContainer = part.toUpperCase();
            }
          }
        }
      } catch (RuntimeException e) {
        Log.d(
            LOG_TAG,
            String.format("Could not parse format data from supplied String: %s", data),
            e);
      }
    }

    /**
     * Parse the video bitrate into a Long
     *
     * @param element HTML DOM containing bitrate data
     * @return The video bitrate (long)
     */
    private long parseBitrate(@NotNull final Element element) {

      // Result container
      long bitrate = 0;

      // Extract data from DOM
      final String bitrateData = cleanMetadata(element.nextSibling().toString()).trim();

      try {
        // Split digit from unit
        final Matcher bitrateMatcher = Pattern.compile("(\\d+)").matcher(bitrateData);
        if (bitrateMatcher.find()) {
          final int digit = Integer.parseInt(bitrateMatcher.group());
          // Parse conversion factor
          if (zkfPatterns.getMbpsMatcher(bitrateData).find()) {
            bitrate = (digit * 1_000_000L);
          } else if (zkfPatterns.getKbpsMatcher(bitrateData).find()) {
            bitrate = (digit * 1_000L);
          }
        }
      } catch (NumberFormatException e) {
        Log.d(LOG_TAG, "Error parsing bitrate data", e);
      } finally {
        // Ensure bitrate has been set
        if (bitrate == 0) {
          Log.d(
              LOG_TAG,
              String.format(
                  "Could not parse EventFileSource bitrate from String: %s; defaulting to 4MBps",
                  bitrate));
          bitrate = zkfPatterns.getDefaultBitrate();
        }
      }

      return bitrate;
    }

    /**
     * Parse approximate file size from HTML DOM
     *
     * @param element An HTML DOM
     * @return Approximate file size of this video source
     */
    private Long parseFileSize(@NotNull final Element element) {

      final String data = cleanMetadata(element.nextSibling().toString());

      Long result = null;

      // Americanize
      final String decimalData = data.replace(",", ".");

      final Matcher matcher = zkfPatterns.getFilesizeMatcher(decimalData);
      if (matcher.find()) {
        final float size = Float.parseFloat(matcher.group(1));
        final String units = matcher.group(2).toUpperCase();
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
     * Remove non-breaking spaces & excess whitespace
     *
     * @param metadata The string to be cleaned
     * @return A clean String.
     */
    private @NotNull String cleanMetadata(@NotNull final String metadata) {
      return metadata.replace("&nbsp;", "").trim();
    }
  }
}
