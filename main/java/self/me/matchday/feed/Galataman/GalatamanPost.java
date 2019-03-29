/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.Galataman;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Contract;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import self.me.matchday.feed.BloggerPost;
import self.me.matchday.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static self.me.matchday.feed.Galataman.GalatamanPattern.*;

/**
 * Represents an individual post on the Galataman website. Extends the BloggerPost
 * class, and contains additional processors to extract metadata relevant to this
 * website.
 *
 * @author tomas
 */
public final class GalatamanPost extends BloggerPost
{
    private static final String LOG_TAG = "GalatamanPost";


    // Fields
    // -------------------------------------------------------------------------
    private final List<GalatamanMatchSource> sources;

    // Constructor
    // -------------------------------------------------------------------------
    private GalatamanPost(GalatamanPostBuilder builder)
    {
        // Call superclass constructor
        super(builder);

        // One extra step: to copy over parsed Galataman-specific content,
        // making sure List is immutable
        this.sources = Collections.unmodifiableList( builder.sources );
    }
    
    
    // Getters
    // -------------------------------------------------------------------------
    @Contract(pure = true)
    public List<GalatamanMatchSource> getSources() {
        return this.sources;
    }

    
    // Overridden methods
    // -------------------------------------------------------------------------
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        // Add newly analyzed info to previous String output
        sb
            .append( super.toString() )
            .append("\nSources:\n");
        // Add each source
        this
            .sources
            .forEach(sb::append);

        return sb.toString();
    }


    // Builder
    // ---------------------------------------------------------------------------------
    /**
     * Parses Galataman-specific content and constructs a fully-formed
     * GalatamanPost object.
     *
     * */
    static class GalatamanPostBuilder extends BloggerPostBuilder
    {
        private final List<GalatamanMatchSource> sources = new ArrayList<>();
        private final IMatchSourceProcessor sourceProcessor;

        // Constructor
        GalatamanPostBuilder(JsonObject bloggerPost, IMatchSourceProcessor sourceProcessor)
        {
            super(bloggerPost);

            // Save source processor
            this.sourceProcessor = sourceProcessor;
        }

        /**
         * Extracts match source data from this post.
         *
         */
        private void parseMatchSources()
        {
            try {
                // DOM-ify HTML content for easy manipulation
                Document doc = Jsoup.parse( this.getContent() );

                // Since the document is loosely structured, we will walk through
                // it using a token, starting at the first source and looking
                // for what we want along the way
                Element token =
                        doc.getElementsMatchingOwnText(START_OF_SOURCE).first();

                // Search until the end of the Document
                while (token != null) {
                    // When we find a source
                    if (isSourceData.test(token)) {
                        // Save HTML
                        String html = token.html();
                        // URLS for this source
                        List<URL> urls = new ArrayList<>();

                        // Now, continue searching, this time for links,
                        // until the next source or the end of the HTML
                        Element innerToken = token.nextElementSibling();
                        while( (innerToken != null) && !(isSourceData.test(innerToken)) )
                        {
                            // When we find a link to a video file
                            if ( isVideoLink.test(innerToken) )
                                // Extract href attribute & add it to our
                                // source's list of URLs
                                urls.add(
                                        new URL( innerToken.attr("href") )
                                );

                            // Advance inner token
                            innerToken = innerToken.nextElementSibling();
                        }

                        // Parse dara
                        GalatamanMatchSource source = sourceProcessor.parse(html, urls);

                        // Add match source to object
                        this.sources.add(source);
                    }

                    // Advance the search token
                    token = token.nextElementSibling();
                }

            } catch (MalformedURLException e) {
                // Something went wrong extracting a source link
                GalatamanPostParseException exception
                        = new GalatamanPostParseException(
                                    "Could not extract source link from post:" + this.getBloggerPostID(),
                                    e
                            );
                // Log the error
                Log.e(LOG_TAG, "Error parsing links from GalatamanPost", exception);
                // Rethrow
                throw exception;

            } catch (RuntimeException e) {
                // Wrap exception
                GalatamanPostParseException exception
                        = new GalatamanPostParseException(
                                "Error while parsing: " + this.getBloggerPostID() +
                                " at: " + this.getLink(),
                                e
                            );
                // Log the error
                Log.e(LOG_TAG, "There was a problem parsing a Galataman post", exception);
                // Rethrow exception
                throw exception;
            }
        }

        /**
         * Build the GalatamanPost object
         *
         * @return GalatamanPost - A fully-formed post object
         */
        @Override
        public GalatamanPost build()
        {
            // Super methods
            // -----------------------------------------------
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
            // -----------------------------------------------
            // Parse content
            parseMatchSources();
            // Construct a fully-formed GalatamanPost object
            return new GalatamanPost(this);
        }
    }
}
