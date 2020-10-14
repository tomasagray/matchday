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
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventFileSource.Resolution;
import self.me.matchday.model.FileSize;
import self.me.matchday.util.BeanLocator;
import self.me.matchday.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZKFFileSourceMetadataParser {

  private static final String LOG_TAG = "ZKFFileSourceMetadataParser";

  // Metadata tags
  private static final String CHANNEL = "channel:";
  private static final String LANGUAGE = "language:";
  private static final String FORMAT = "format:";
  private static final String BITRATE = "bitrate:";
  private static final String SIZE = "size:";

  public static EventFileSource createFileSource(@NotNull final Elements elements) {

    final ZKFFileSourceMetadataParser parser = new ZKFFileSourceMetadataParser(elements);
    return
        EventFileSource
            .builder()
            .channel(parser.channel)
            .languages(parser.languages)
            .resolution(parser.resolution)
            .frameRate(parser.frameRate)
            .mediaContainer(parser.mediaContainer)
            .bitrate(parser.bitrate)
            .fileSize(parser.fileSize)
            .eventFiles(new ArrayList<>())
            .build();
  }

  private final ZKFPatterns zkfPatterns;
  private String channel;
  private final List<String> languages = new ArrayList<>();
  private Resolution resolution;
  private int frameRate;
  private String mediaContainer;
  private long bitrate;
  private Long fileSize;

  private ZKFFileSourceMetadataParser(@NotNull final Elements elements) {

    // Get pattern container
    this.zkfPatterns = BeanLocator.getBean(ZKFPatterns.class);
    parseEventMetadata(elements);
  }

  /**
   * Parse a collection of elements into metadata for this Event
   *
   * @param elements A Collection of Jsoup elements
   */
  private void parseEventMetadata(@NotNull final Elements elements) {

    elements.forEach(element -> {
      // analyze metadata tag
      final String text = element.text();
      switch (text) {
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
          final String approxFileSize = cleanMetadata(element.nextSibling().toString());
          this.fileSize = parseFileSize(approxFileSize);
          break;
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
        if (zkfPatterns.getMbpsMatcher(bitrate).find()) {
          this.bitrate = (digit * 1_000_000L);
        } else if (zkfPatterns.getKbpsMatcher(bitrate).find()) {
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
        this.bitrate = zkfPatterns.getDefaultBitrate();
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

  private Long parseFileSize(@NotNull final String data) {

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
