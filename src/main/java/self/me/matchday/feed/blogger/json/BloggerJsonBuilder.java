package self.me.matchday.feed.blogger.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.blogger.Blogger;
import self.me.matchday.feed.blogger.BloggerPost;
import self.me.matchday.feed.blogger.IBloggerBuilder;
import self.me.matchday.feed.blogger.InvalidBloggerFeedException;
import self.me.matchday.io.JsonStreamReader;

public class BloggerJsonBuilder implements IBloggerBuilder {

  private final JsonPostBuilderFactory postBuilderFactory;
  private final JsonObject blog;

  // Fields
  private final String blogId;
  private final String title;
  private String version;
  private final String author;
  private final String link;
  private final Stream<BloggerPost> posts;
  private long postCount;

  // Constructor
  public BloggerJsonBuilder(
      URL url, JsonPostBuilderFactory postBuilderFactory) throws IOException {

    // Save constructor args
    this.postBuilderFactory = postBuilderFactory;
    // Read the blog
    this.blog = parseFeed(JsonStreamReader.readRemote(url));
    // Parse the blog metadata
    this.blogId = parseId();
    this.title = parseTitle();
    this.author = parseAuthor();
    this.link = parseLink();

    // Parse blog entries; ensure the list is immutable
    this.posts = parseEntries();
  }

  /**
   * Get the "feed" portion of the JSON object. Also sets version field, as this is the only data
   * outside of the "feed" structure.
   *
   * @param feed A JsonObject of the entire JSON feed
   */
  private JsonObject parseFeed(@NotNull final JsonObject feed) {

    try {
      this.version = feed.get("version").getAsString();
      return feed.get("feed").getAsJsonObject();
    } catch (NullPointerException e) {
      throw new InvalidBloggerFeedException("Unable to get 'feed' object", e);
    }
  }

  /**
   * Get the top-level Blogger ID.
   *
   * @return String representing the Blog's ID.
   */
  private String parseId() {

    String id;
    try {
      id = this.blog.get("id").getAsJsonObject().get("$t").getAsString();
    } catch (NullPointerException e) {
      throw new InvalidBloggerFeedException("Could not parse blog ID", e);
    }
    return id;
  }

  /**
   * Extract the title of the blog.
   *
   * @return String The blog title.
   */
  private String parseTitle() {

    String title;
    // Attempt to extract the title
    try {
      title =
          this
              .blog
              .get("title")
              .getAsJsonObject()
              .get("$t")
              .getAsString();
    } catch (NullPointerException e) {
      throw new InvalidBloggerFeedException("Could not parse blog title", e);
    }
    return title;
  }

  /**
   * Get the version info as a String.
   *
   * @return String The blog version.
   */
  private String parseVersion() {

    String version;
    // Attempt to parse version info
    try {
      version = this.blog.get("version").getAsString();
    } catch (NullPointerException e) {
      throw new InvalidBloggerFeedException("Could not parse blog version info", e);
    }
    return version;
  }

  /**
   * Extract the author's name.
   *
   * @return String The author's name.
   */
  private String parseAuthor() {

    String author;
    // Attempt to extract author's name
    try {
      author =
          this
              .blog
                .get("author")
                .getAsJsonArray()
                .get(0)
                .getAsJsonObject()
                .get("name")
                .getAsJsonObject()
                .get("$t")
                .getAsString();
    } catch (NullPointerException e) {
      throw new InvalidBloggerFeedException("Could not parse blog author info", e);
    }
    return author;
  }

  /**
   * Get a human-readable link to the blog.
   *
   * @return String A link to the blog.
   */
  private String parseLink() {

    String link = null;
    // Extract link
    try {
      JsonArray links = this.blog.get("link").getAsJsonArray();
      for (JsonElement lnk : links) {
        JsonObject linkObj = lnk.getAsJsonObject();
        String linkType = linkObj.get("rel").getAsString();

        if ("alternate".equals(linkType)) {
          link = linkObj.get("href").getAsString();
        }
      }

    } catch (NullPointerException e) {
      throw new InvalidBloggerFeedException("Could not parse blog link", e);
    }
    return link;
  }

  /**
   * Examine the JSON stream for blog entries. For each entry found, create a new BloggerPost
   * object, and add it to this object's collection.
   */
  @NotNull
  private Stream<BloggerPost> parseEntries() {

    // Result container for entries
    List<BloggerPost> entries = new ArrayList<>();
    // Get entries
    JsonArray blogEntries = this.blog.get("entry").getAsJsonArray();

    // Ensure the feed contains at least one entry
    if (blogEntries.isJsonNull() || blogEntries.size() == 0) {
      throw new InvalidBloggerFeedException("Feed is empty");
    }
    // Iterate over blogEntries
    blogEntries.forEach(
        (entry) -> {
          // Ensure valid JSON;
          if (entry.isJsonObject()) {
            // Create a post parser
            final BloggerJsonPostBuilder postBuilder =
                postBuilderFactory.createPostBuilder(entry.getAsJsonObject());
            // Parse entry & add to collection
            entries.add(postBuilder.getBloggerPost());
            postCount++;
          }
        });

    // Return a Stream of BloggerPosts
    return
        entries.stream();
  }

  @Override
  public Blogger getBlogger() {
    return
        new Blogger(blogId, title, version, author, link, posts, postCount);
  }
}
