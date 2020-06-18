package self.me.matchday.feed.blogger.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import self.me.matchday.feed.blogger.BloggerPost;
import self.me.matchday.feed.blogger.IBloggerPostBuilder;
import self.me.matchday.feed.blogger.InvalidBloggerPostException;
import self.me.matchday.util.Log;

/**
 * Constructs a fully-formed BloggerPost object which is then passed to the BloggerPost
 * constructor.
 */
public class BloggerJsonPostBuilder implements IBloggerPostBuilder {

  private static final String LOG_TAG = "BloggerJsonPostBuilder";

  private final JsonObject postJson;
  private final String bloggerPostID;
  private final String title;
  private final String content;
  private final String link;
  private final LocalDateTime published;
  private final LocalDateTime lastUpdated;
  private final List<String> categories = new ArrayList<>();

  public BloggerJsonPostBuilder(@NotNull final JsonObject postJson) {

    this.postJson = postJson;
    // Parse each element of the post (entry)
    this.bloggerPostID = parsePostID();
    this.published = parsePublished();
    this.title = parseTitle();
    this.link = parseLink();
    this.content = parseContent();
    this.lastUpdated = parseLastUpdated();
    parseCategories();
  }

  /**
   * Get the Post ID
   */
  private String parsePostID() {
    try {
      return
          this.postJson
              .get("id")
              .getAsJsonObject()
              .get("$t")
              .getAsString();
    } catch (NullPointerException e) {
      // No post ID found - ABORT!
      throw new InvalidBloggerPostException("Could not determine post ID", e);
    }
  }

  /**
   * Get the Post's initially published date
   */
  private LocalDateTime parsePublished() {
    try {
      return parseDateTimeString("published");

    } catch (NullPointerException | DateTimeParseException e) {
      throw new InvalidBloggerPostException("Could not parse published date", e);
    }
  }

  /**
   * Get the Post title
   */
  private String parseTitle() {
    try {
      return
          this.postJson
              .get("title")
              .getAsJsonObject()
              .get("$t")
              .getAsString();

    } catch (NullPointerException e) {
      throw new InvalidBloggerPostException("Could not parse post title", e);
    }
  }

  /**
   * Get the link to the Post
   */
  private @Nullable String parseLink() {

    try {
      JsonArray links = this.postJson.get("link").getAsJsonArray();
      // Search the array of links for the one we want
      for (JsonElement link : links) {
        // Find the 'alternate' link
        String linkType = link.getAsJsonObject().get("rel").getAsString();
        if ("alternate".equals(linkType)) {
          // Assign the link
          return link.getAsJsonObject().get("href").getAsString();
        }
      }
    } catch (NullPointerException e) {
      // Wrap as a more descriptive exception
      throw new InvalidBloggerPostException("Could not parse post link", e);
    }
    return null;
  }

  /**
   * Parse the Blogger content field (the post HTML).
   */
  private String parseContent() {
    try {
      return
          this.postJson
              .get("content")
              .getAsJsonObject()
              .get("$t")
              .getAsString();

    } catch (NullPointerException e) {
      throw new InvalidBloggerPostException("Could not parse post content", e);
    }
  }

  /**
   * Get update timestamp
   */
  private @Nullable LocalDateTime parseLastUpdated() {
    try {
      return parseDateTimeString("updated");
    } catch (NullPointerException | DateTimeParseException e) {
      Log.e(LOG_TAG, "Could not parse UPDATE DATE data for BloggerPost: " + bloggerPostID, e);
      return null;
    }
  }

  /**
   * Get categories for this Post (teams, competition)
   */
  private void parseCategories() {
    try {
      this.postJson
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
    return
        OffsetDateTime
            .parse(
                this.postJson
                    .get(timeString)
                    .getAsJsonObject()
                    .get("$t")
                    .getAsString(),
        DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        .withNano(0)
        .toLocalDateTime();
  }

  @Override
  public BloggerPost getBloggerPost() {
    return
        new BloggerPost(bloggerPostID, published, lastUpdated, categories, title, content, link);
  }
}
