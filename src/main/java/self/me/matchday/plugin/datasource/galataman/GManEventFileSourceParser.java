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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFile.EventPartIdentifier;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.plugin.datasource.bloggerparser.EventFileSourceParser;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a specific source for an Event (for example: 1080i, Spanish), derived from the
 * Galataman blog (http://galatamanhdf.blogspot.com/)
 *
 * @author tomas
 */
@Component
public final class GManEventFileSourceParser implements EventFileSourceParser {

  private final GManPatterns gManPatterns;
  private final GManFileSourceMetadataParser fileSourceMetadataParser;

  @Autowired
  public GManEventFileSourceParser(
      final GManPatterns gManPatterns,
      final GManFileSourceMetadataParser fileSourceMetadataParser) {

    this.gManPatterns = gManPatterns;
    this.fileSourceMetadataParser = fileSourceMetadataParser;
  }

  @Override
  public List<EventFileSource> getEventFileSources(@NotNull final String html) {
    return parseEventSources(html);
  }

  /** Extracts match source data from this post. */
  private @NotNull List<EventFileSource> parseEventSources(@NotNull final String html) {

    // Result container
    final List<EventFileSource> fileSources = new ArrayList<>();

    // DOM-ify HTML content for easy manipulation
    Document doc = Jsoup.parse(html);
    // Since this is a loosely structured document, we will use a token, starting at the first
    // source and looking for what we want along the way
    Element token = doc.getElementsMatchingOwnText(gManPatterns.getStartOfMetadata()).first();

    // Search until the end of the Document
    while (token != null) {
      // When we find a source
      if (gManPatterns.isSourceData(token)) {

        // Create an Event file source from the data
        final EventFileSource eventFileSource =
            fileSourceMetadataParser.createFileSource(token.html());

        // Parse EventFiles (links) for this source
        Element innerToken = token.nextElementSibling();
        EventPartIdentifier partIdentifier = EventPartIdentifier.DEFAULT;
        while ((innerToken != null) && !(gManPatterns.isSourceData(innerToken))) {

          // Look for a part identifier
          final String tokenHtml = innerToken.html();
          if (EventPartIdentifier.isPartIdentifier(tokenHtml)) {

            // Create an identifier for this part
            partIdentifier = EventPartIdentifier.fromString(tokenHtml);
          } else if (gManPatterns.isVideoLink(innerToken)) {
            try {
              // When we find a link to a video file, extract href attribute & add it to our
              // source's list of EventFiles, with an identifier (might be null)
              final URL url = new URL(innerToken.attr("href"));
              // Create a new EventFile & add to collection
              final EventFile eventFile = new EventFile(partIdentifier, url);
              eventFileSource.getEventFiles().add(eventFile);
            } catch (MalformedURLException ignore) {
            }
          }
          // Advance inner token
          innerToken = innerToken.nextElementSibling();
        }
        // Add match source to collection
        fileSources.add(eventFileSource);
      }
      // Advance the search token
      token = token.nextElementSibling();
    }

    return fileSources;
  }
}
