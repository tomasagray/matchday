/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.blogger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Log;

/**
 * Represents an individual, generic post on a Blogger blog. The constructor accepts either a Gson
 * JsonObject or a JSON string.
 *
 * <p>This class can be extended to allow it to be customized to a particular blog.
 *
 * @author tomas
 */
@Data
public abstract class BloggerPost {
  private static final String LOG_TAG = "BloggerPostClass";

  // Fields
  private final String bloggerPostID;
  private final LocalDateTime published;
  private final LocalDateTime lastUpdated;
  private final List<String> categories;
  private final String title;
  private final String content;
  private final String link;

  // Constructor
  @Contract(pure = true)
  protected BloggerPost(@NotNull BloggerPostBuilder builder) {
    // Initialize our object with a properly initialized copy
    this.bloggerPostID = builder.bloggerPostID;
    this.published = builder.published;
    this.lastUpdated = builder.lastUpdated;
    // Ensure categories List is immutable
    this.categories = Collections.unmodifiableList(builder.categories);
    this.title = builder.title;
    this.content = builder.content;
    this.link = builder.link;
  }



  // Overridden methods
  @Override
  public String toString() {
    // Ensure missing LocalDateTime fields do not
    // cause NullPointerExceptions
    String nullSignifier = "{NULL}";
    String l_published = this.published != null ? this.published.toString() : nullSignifier;
    String l_updated = this.lastUpdated != null ? this.lastUpdated.toString() : nullSignifier;

    return "[\n"
        + "\tid: "
        + this.bloggerPostID
        + "\n"
        + "\tpublished: "
        + l_published
        + "\n"
        + "\tupdated: "
        + l_updated
        + "\n"
        + "\ttitle: "
        + this.title
        + "\n"
        + "\tlink: "
        + this.link
        + "\n"
        + "\tcategories: "
        + this.categories
        + "\n"
        + "]";
  }

  /**
   * Constructs a fully-formed BloggerPost object which is then passed to the BloggerPost
   * constructor.
   */
  public static abstract class BloggerPostBuilder {
    private final JsonObject bloggerPost;
    private String bloggerPostID;
    private String title;
    private String content;
    private String link;
    private LocalDateTime published;
    private LocalDateTime lastUpdated;
    private final List<String> categories =
        new ArrayList<>(); // will be empty if no categories parsed

    // Constructor
    public BloggerPostBuilder(JsonObject bloggerPost) {
      this.bloggerPost = bloggerPost;
    }

    // Parsers for MANDATORY fields
    /** Get the Post ID */
    protected void parsePostID() {
      try {
        this.bloggerPostID = this.bloggerPost.get("id").getAsJsonObject().get("$t").getAsString();
      } catch (NullPointerException e) {
        // No post ID was found - ABORT!
        throw new InvalidBloggerPostException("Could not determine post ID", e);
      }
    }

    /** Get the date Post was initially published */
    protected void parsePublished() {
      try {
        this.published = parseDateTimeString("published");

      } catch (NullPointerException | DateTimeParseException e) {
        throw new InvalidBloggerPostException("Could not parse published date", e);
      }
    }

    /** Get the Post title */
    protected void parseTitle() {
      try {
        this.title = this.bloggerPost.get("title").getAsJsonObject().get("$t").getAsString();

      } catch (NullPointerException e) {
        String msg = "Could not parse post title";
        throw new InvalidBloggerPostException(msg, e);
      }
    }

    /** Get the link to the Post */
    protected void parseLink() {
      try {
        JsonArray links = this.bloggerPost.get("link").getAsJsonArray();
        // Search the array of links for the one we want
        for (JsonElement link : links) {
          // Find the 'alternate' link
          String linkType = link.getAsJsonObject().get("rel").getAsString();
          if ("alternate".equals(linkType))
            // Assign the link
            this.link = link.getAsJsonObject().get("href").getAsString();
        }

        // If we didn't find it
        if (this.link == null) {
          throw new InvalidBloggerPostException("Could not parse 'alternate' link");
        }

      } catch (NullPointerException e) {
        // Wrap as a more descriptive exception
        throw new InvalidBloggerPostException("Could not parse post link", e);
      }
    }

    protected void parseContent() {
      try {
        this.content = this.bloggerPost.get("content").getAsJsonObject().get("$t").getAsString();

      } catch (NullPointerException e) {
        throw new InvalidBloggerPostException("Could not parse post content", e);
      }
    }

    // Parsers for optional fields
    // --------------------------------------------------------------------------

    /** Get update timestamp */
    protected void parseLastUpdated() {
      try {
        this.lastUpdated = parseDateTimeString("updated");

      } catch (NullPointerException | DateTimeParseException e) {
        Log.e(
            LOG_TAG, "Could not parse UPDATE DATE data for BloggerPost: " + getBloggerPostID(), e);
      }
    }

    /** Get categories for this Post (teams, competition) */
    protected void parseCategories() {
      try {
        this.bloggerPost
            .get("category")
            .getAsJsonArray()
            .forEach((c) -> this.categories.add(c.getAsJsonObject().get("term").getAsString()));
      } catch (NullPointerException | IllegalStateException | ClassCastException e) {
        Log.e(LOG_TAG, "Could not parse CATEGORY data for BloggerPost: " + getBloggerPostID(), e);
      }
    }

    /**
     * Extract a LocalDateTime from the post object.
     *
     * @param timeString The identifier of the timestamp
     * @return A LocalDateTime object, with nano set to 0
     */
    private LocalDateTime parseDateTimeString(String timeString) {
      return OffsetDateTime.parse(
              this.bloggerPost.get(timeString).getAsJsonObject().get("$t").getAsString(),
              DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          .withNano(0)
          .toLocalDateTime();
    }

    // Expose data for use by subclasses
    protected String getContent() {
      return this.content;
    }

    protected String getBloggerPostID() {
      return this.bloggerPostID;
    }

    protected String getLink() {
      return this.link;
    }

    /**
     * Call each build method, to ensure a properly constructed BloggerPost object is created
     *
     * @return A fully-formed, properly constructed BloggerPost
     */
    public abstract BloggerPost build();
  }
}
