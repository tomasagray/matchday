/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.plugin.datasource.galataman;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import self.me.matchday.plugin.datasource.EventFileSourceParser;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFile.EventPartIdentifier;
import self.me.matchday.model.EventFileSource;

/**
 * Represents a specific source for an Event (for example: 1080i, Spanish), derived from the
 * Galataman blog (http://galatamanhdf.blogspot.com/)
 *
 * @author tomas
 */
public final class GManEventFileSourceParser implements EventFileSourceParser {

  private final String html;
  private final List<EventFileSource> eventFileSources;

  public GManEventFileSourceParser(@NotNull final String html) {
    this.html = html;
    this.eventFileSources = parseEventSources();
  }

  @Override
  public List<EventFileSource> getEventFileSources() {
    return eventFileSources;
  }

  /**
   * Extracts match source data from this post.
   */
  private @NotNull List<EventFileSource> parseEventSources() {

    // Result container
    final List<EventFileSource> fileSources = new ArrayList<>();

    // DOM-ify HTML content for easy manipulation
    Document doc = Jsoup.parse(this.html);
    // Since this is a loosely structured document, we will use a token, starting at the first
    // source and looking for what we want along the way
    Element token = doc.getElementsMatchingOwnText(GManPatterns.START_OF_SOURCE).first();

    // Search until the end of the Document
    while (token != null) {
      // When we find a source
      if (GManPatterns.isSourceData(token)) {
        // Create an Event file source from the data
        final EventFileSource eventFileSource =
            GManFileSourceMetadataParser.createFileSource(token.html());

        // Parse EventFiles (links) for this source
        Element innerToken = token.nextElementSibling();
        EventPartIdentifier partIdentifier = EventPartIdentifier.DEFAULT;
        while ((innerToken != null) && !(GManPatterns.isSourceData(innerToken))) {

          // Look for a part identifier
          final String tokenHtml = innerToken.html();
          if (EventPartIdentifier.isPartIdentifier(tokenHtml)) {

            // Create an identifier for this part
            partIdentifier = EventPartIdentifier.fromString(tokenHtml);
          } else if (GManPatterns.isVideoLink(innerToken)) {
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
