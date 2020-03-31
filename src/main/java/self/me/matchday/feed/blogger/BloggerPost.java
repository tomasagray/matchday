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
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Log;

/**
 * Represents an individual, generic post on a Blogger blog. The constructor accepts either a Gson
 * JsonObject, or a JSON string.
 * <p>
 * This class can be extended to allow it to be customized to a particular blog.
 *</p>
 *
 * @author tomas
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BloggerPost {

  private static final String LOG_TAG = "BloggerPostClass";

  // Fields
  private String bloggerPostID;
  private LocalDateTime published;
  private LocalDateTime lastUpdated;
  private List<String> categories;
  private String title;
  private String content;
  private String link;

  // Default constructor, for JPA
  /*public BloggerPost() {
    bloggerPostID = null;
    published = null;
    lastUpdated = null;
    categories = null;
    title = null;
    content = null;
    link = null;
  }*/

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
  public static class BloggerPostBuilder implements IBloggerPostProcessor {

    private JsonObject bloggerPost;
    private String bloggerPostID;
    private String title;
    private String content;
    private String link;
    private LocalDateTime published;
    private LocalDateTime lastUpdated;
    private final List<String> categories = new ArrayList<>();

    // Constructor
//    private BloggerPostBuilder(JsonObject bloggerPost) {
//      this.bloggerPost = bloggerPost;
//    }

    // Parsers for MANDATORY fields
    /**
     * Get the Post ID
     */
    private void parsePostID() {
      try {
        this.bloggerPostID = this.bloggerPost.get("id").getAsJsonObject().get("$t").getAsString();
      } catch (NullPointerException e) {
        // No post ID found - ABORT!
        throw new InvalidBloggerPostException("Could not determine post ID", e);
      }
    }

    /**
     * Get the Post's initially published date
     */
    private void parsePublished() {
      try {
        this.published = parseDateTimeString("published");

      } catch (NullPointerException | DateTimeParseException e) {
        throw new InvalidBloggerPostException("Could not parse published date", e);
      }
    }

    /**
     * Get the Post title
     */
    private void parseTitle() {
      try {
        this.title = this.bloggerPost.get("title").getAsJsonObject().get("$t").getAsString();

      } catch (NullPointerException e) {
        throw new InvalidBloggerPostException("Could not parse post title", e);
      }
    }

    /**
     * Get the link to the Post
     */
    private void parseLink() {
      try {
        JsonArray links = this.bloggerPost.get("link").getAsJsonArray();
        // Search the array of links for the one we want
        for (JsonElement link : links) {
          // Find the 'alternate' link
          String linkType = link.getAsJsonObject().get("rel").getAsString();
          if ("alternate".equals(linkType))
          // Assign the link
          {
            this.link = link.getAsJsonObject().get("href").getAsString();
          }
        }
      } catch (NullPointerException e) {
        // Wrap as a more descriptive exception
        throw new InvalidBloggerPostException("Could not parse post link", e);
      }
    }

    private void parseContent() {
      try {
        this.content = this.bloggerPost.get("content").getAsJsonObject().get("$t").getAsString();

      } catch (NullPointerException e) {
        throw new InvalidBloggerPostException("Could not parse post content", e);
      }
    }

    /**
     * Get update timestamp
     */
    private void parseLastUpdated() {
      try {
        this.lastUpdated = parseDateTimeString("updated");
      } catch (NullPointerException | DateTimeParseException e) {
        Log.e(LOG_TAG, "Could not parse UPDATE DATE data for BloggerPost: " + bloggerPostID, e);
      }
    }

    /**
     * Get categories for this Post (teams, competition)
     */
    private void parseCategories() {
      try {
        this.bloggerPost
            .get("category")
            .getAsJsonArray()
            .forEach((c) -> this.categories.add(c.getAsJsonObject().get("term").getAsString()));
      } catch (NullPointerException | IllegalStateException | ClassCastException e) {
        Log.e(LOG_TAG, "Could not parse CATEGORY data for BloggerPost: " + bloggerPostID, e);
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

    // Build object
    @NotNull
    @Contract(" -> new")
    private BloggerPost buildPost() {

      // Parse each element of the post (entry)
      parsePostID();
      parsePublished();
      parseTitle();
      parseLink();
      parseContent();
      parseLastUpdated();
      parseCategories();

      // Construct post
      return
          new BloggerPost(bloggerPostID, published, lastUpdated, categories, title, content, link);
    }

    @Override
    public BloggerPost parse(JsonObject entry) {
      this.bloggerPost = entry;
      return buildPost();
    }
  }
}
