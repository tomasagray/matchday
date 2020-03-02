/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.blogger.galataman;

import static self.me.matchday.feed.blogger.galataman.GalatamanPattern.START_OF_SOURCE;
import static self.me.matchday.feed.blogger.galataman.GalatamanPattern.isSourceData;
import static self.me.matchday.feed.blogger.galataman.GalatamanPattern.isVideoLink;

import com.google.gson.JsonObject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import self.me.matchday.feed.blogger.BloggerPost;
import self.me.matchday.feed.blogger.galataman.GalatamanEventFileSource.GalatamanEventFileSourceBuilder;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFile.EventPartIdentifier;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.util.Log;

/**
 * Represents an individual post on the Galataman website. Extends the BloggerPost class, and
 * contains additional processors to extract metadata relevant to this website.
 *
 * @author tomas
 */
@Entity
public final class GalatamanPost extends BloggerPost {

  private static final String LOG_TAG = "GalatamanPost";

  // Fields
  @OneToOne(cascade = CascadeType.MERGE)
  private final Event event; // the Event represented by this Post
  @ElementCollection
  @OneToMany(cascade = CascadeType.MERGE)
  private final List<EventFileSource> eventFileSources;

  public GalatamanPost() {
    super();
    this.event = null;
    this.eventFileSources = null;
  }

  // Constructor
  private GalatamanPost(GalatamanPostBuilder builder) {
    // Call superclass constructor
    super(builder);

    // Copy over parsed Galataman-specific content, making sure List is immutable
    this.eventFileSources = Collections.unmodifiableList(builder.sources);
    // Extract Event metadata
    this.event = new GalatamanEventDataParser(this).getEvent();
  }

  // Overridden methods
  @Override
  public Event getEvent() {
    return this.event;
  }

  @Override
  public List<EventFileSource> getEventFileSources() {
    return this.eventFileSources;
  }

  @NotNull
  @Override
  public String toString() {
    return
        String.format("%s (%s), %s sources", getTitle(), getLink(), getEventFileSources().size());
  }

  /**
   * Parses Galataman-specific content and constructs a fully-formed GalatamanPost object.
   */
  static class GalatamanPostBuilder extends BloggerPostBuilder {

    private final List<GalatamanEventFileSource> sources = new ArrayList<>();

    // Constructor
    GalatamanPostBuilder(JsonObject bloggerPost) {
      super(bloggerPost);
    }

    /**
     * Extracts match source data from this post.
     */
    private void parseMatchSources() {
      try {
        // DOM-ify HTML content for easy manipulation
        Document doc = Jsoup.parse(this.getContent());

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
                eventFiles.add(
                    new EventFile(partIdentifier, new URL(innerToken.attr("href")))
                );
              }

              // Advance inner token
              innerToken = innerToken.nextElementSibling();
            }

            // Parse data into file sources
            GalatamanEventFileSource fileSources =
                new GalatamanEventFileSourceBuilder(metadata, eventFiles).build();

            // Add match source to object
            this.sources.add(fileSources);
          }

          // Advance the search token
          token = token.nextElementSibling();
        }

      } catch (MalformedURLException e) {
        // Something went wrong extracting a source link
        GalatamanPostParseException exception =
            new GalatamanPostParseException(
                "Could not extract source link from post:" + this.getBloggerPostID(), e);
        // Log the error
        Log.e(LOG_TAG, "Error parsing links from GalatamanPost", exception);
        // Rethrow
        throw exception;

      } catch (RuntimeException e) {
        // Wrap exception
        GalatamanPostParseException exception =
            new GalatamanPostParseException(
                "Error while parsing: " + this.getBloggerPostID() + " at: " + this.getLink(), e);
        // Log the error
        Log.e(LOG_TAG, "There was a problem parsing a Galataman post", exception);
        // Rethrow exception
        //        throw exception;
      }
    }

    /**
     * Build the GalatamanPost object
     *
     * @return GalatamanPost - A fully-formed post object
     */
    @Override
    public GalatamanPost build() {
      // Super methods
      // Mandatory fields
      parsePostID();
      parsePublished();
      parseTitle();
      parseLink();
      parseContent();
      // Optional fields
      parseLastUpdated();
      parseCategories();

      // Galataman-specific
      // Parse content
      parseMatchSources();

      // Construct a fully-formed GalatamanPost object
      return new GalatamanPost(this);
    }
  }

}
