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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.FileServerService;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFile.EventPartIdentifier;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.plugin.datasource.bloggerparser.EventFileSourceParser;
import self.me.matchday.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Implementation of the Event File Source parser, specific to the ZKFootball blog, found at:
 * https://zkfootballmatch.blogspot.com
 */
@Component
public class ZKFEventFileSourceParser implements EventFileSourceParser {

  private static final String LOG_TAG = "ZKFEventFileSourceParser";

  private final ZKFPatterns zkfPatterns;
  private final ZKFFileSourceMetadataParser metadataParser;
  private final FileServerService fileServerService;

  public ZKFEventFileSourceParser(
      @Autowired final ZKFPatterns zkfPatterns,
      @Autowired final ZKFFileSourceMetadataParser metadataParser,
      @Autowired final FileServerService fileServerService) {

    this.zkfPatterns = zkfPatterns;
    this.metadataParser = metadataParser;
    this.fileServerService = fileServerService;
  }

  @Override
  public List<EventFileSource> getEventFileSources(@NotNull final String html) {
    return parseEventFileSources(html);
  }

  /**
   * Parse out all EventFileSources from this post
   *
   * @return A List<> of EventFileSources (may be empty)
   */
  private @NotNull List<EventFileSource> parseEventFileSources(@NotNull final String html) {

    // Prevent XSS attacks & parse HTML
    final String content = Jsoup.clean(html, Whitelist.basic());
    final Document dom = Jsoup.parse(content);

    // EventFileSources container
    final List<EventFileSource> fileSources = new ArrayList<>();
    // EventFiles container
    final Set<EventFile> eventFiles = new TreeSet<>();

    // Get all potential metadata identifiers
    final Elements elements = dom.select("b");
    elements.forEach(
        element -> {
          final String elementText = element.ownText();
          // EventFile
          if (EventPartIdentifier.isPartIdentifier(elementText)) {

            // Get part identifier
            final EventPartIdentifier partIdentifier = EventPartIdentifier.fromString(elementText);
            final Optional<Element> linkOptional = getNextLink(element);
            linkOptional.ifPresent(
                link -> {
                  // Get link href
                  final String href = link.attr("href");
                  // Attempt to parse
                  try {
                    final URL url = new URL(href);
                    // Ensure link is one the server can parse
                    if (fileServerService.isVideoLink(url)) {
                      // Create EventFile & add to collection
                      final EventFile eventFile = new EventFile(partIdentifier, url);
                      eventFiles.add(eventFile);
                    }
                  } catch (MalformedURLException e) {
                    Log.e(LOG_TAG, "Found a link, but could not parse it: " + href, e);
                  }
                });
          }
          // EventFileSource
          else if (zkfPatterns.isMetadata(element.text())) {

            // Create a file source from data
            final EventFileSource fileSource =
                metadataParser.createFileSource(element.select("span"));
            // Add EventFiles to the current EventFileSource
            fileSource.getEventFiles().addAll(eventFiles);
            // Add to collection
            fileSources.add(fileSource);
            // Reset EventFiles container
            eventFiles.clear();
          }
        });

    return fileSources;
  }

  /**
   * Given a DOM structure element, find the next link (A) after it in the DOM structure
   *
   * @param element The DOM element containing an Event part identifier
   * @return An Optional which contains the link, if it was found
   */
  private Optional<Element> getNextLink(@NotNull final Element element) {

    Optional<Element> result = Optional.empty();
    Element sibling = element.nextElementSibling();

    // Find the next a[href] after the current element, but ending at the next part
    while (sibling != null && !(EventPartIdentifier.isPartIdentifier(sibling.ownText()))) {
      if ("a".equalsIgnoreCase(sibling.tagName())) {
        result = Optional.of(sibling);
        break;
      }
      // advance token
      sibling = sibling.nextElementSibling();
    }
    return result;
  }
}
