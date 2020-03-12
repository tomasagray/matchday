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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.blogger.BloggerPost.BloggerPostBuilder;
import self.me.matchday.io.JsonStreamReader;

/**
 * Represents a Blogger blog.
 *
 * @author tomas
 */
@Getter
public class Blogger {

  // Fields
  private final JsonObject blog;
  private final IBloggerPostProcessor postProcessor;
  private final JsonObject feed;

  private final String blogId;
  private final String title;
  private final String version;
  private final String author;
  private final String link;
  private final Stream<BloggerPost> entries;

  // Factory
  @NotNull
  @Contract("_ -> new")
  public static Blogger fromUrl(@NotNull final URL url) throws IOException {
    return
        new Blogger(url, new BloggerPostBuilder());
  }

  // Constructor
  private Blogger(URL url, IBloggerPostProcessor postProcessor) throws IOException {
    // Save constructor args
    this.postProcessor = postProcessor;

    // Read the blog
    this.blog = JsonStreamReader.readRemote(url);

    // Parse the blog metadata
    this.feed = parseFeed();
    this.blogId = parseId();
    this.title = parseTitle();
    this.version = parseVersion();
    this.author = parseAuthor();
    this.link = parseLink();

    // Parse blog entries; ensure the list is immutable
    this.entries = parseEntries();
  }

  /**
   * Get the "feed" portion of the JSON object.
   * @return JsonObject An object representing the "feed" sub-object.
   */
  private JsonObject parseFeed() {
    JsonObject feed;

    try {
      feed = this.blog.get("feed").getAsJsonObject();
    } catch (NullPointerException e) {
      throw new InvalidBloggerFeedException("Unable to get 'feed' object", e);
    }

    return feed;
  }

  /**
   * Get the top-level Blogger ID.
   * @return String representing the Blog's ID.
   */
  private String parseId() {
    String id;

    try {
      id = feed.get("id").getAsJsonObject().get("$t").getAsString();
    } catch (NullPointerException e) {
      throw new InvalidBloggerFeedException("Could not parse blog ID", e);
    }

    return id;
  }

  /**
   * Extract the title of the blog.
   * @return String The blog title.
   */
  private String parseTitle() {
    String t;
    // Attempt to extract the title
    try {
      t = this.feed.get("title").getAsJsonObject().get("$t").getAsString();

    } catch (NullPointerException e) {
      throw new InvalidBloggerFeedException("Could not parse blog title", e);
    }

    return t;
  }

  /**
   * Get the version info as a String.
   * @return String The blog version.
   */
  private String parseVersion() {
    String v;
    // Attempt to parse version info
    try {
      v = this.blog.get("version").getAsString();
    } catch (NullPointerException e) {
      throw new InvalidBloggerFeedException("Could not parse blog version info", e);
    }

    return v;
  }

  /**
   * Extract the author's name.
   * @return String The author's name.
   */
  private String parseAuthor() {
    String a;
    // Attempt to extract author's name
    try {
      a =
          this.feed
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

    return a;
  }

  /**
   * Get a human-readable link to the blog.
   *
   * @return String A link to the blog.
   */
  private String parseLink() {
    String l = null;
    // Extract link
    try {
      JsonArray links = this.feed.get("link").getAsJsonArray();
      for (JsonElement lnk : links) {
        JsonObject linkObj = lnk.getAsJsonObject();
        String linkType = linkObj.get("rel").getAsString();

        if ("alternate".equals(linkType)) l = linkObj.get("href").getAsString();
      }

    } catch (NullPointerException e) {
      throw new InvalidBloggerFeedException("Could not parse blog link", e);
    }

    return l;
  }

  /**
   * Examine the JSON stream for blog entries. For each entry found, create a new BloggerPost
   * object, and add it to this object's collection.
   */
  @NotNull
  private Stream<BloggerPost> parseEntries() {

    // Result container for entries
    List<BloggerPost> localEntries = new ArrayList<>();

    try {
      JsonArray blogEntries = feed.get("entry").getAsJsonArray();

      // Ensure the feed contains at least one entry
      if (blogEntries.isJsonNull() || blogEntries.size() == 0)
        throw new EmptyBloggerFeedException();

      // Iterate over blogEntries
      blogEntries.forEach(
          (entry) -> {
            // Ensure valid JSON;
            if (entry.isJsonObject()) {
              // Parse entry & add to collection
              localEntries.add(postProcessor.parse(entry.getAsJsonObject()));
            }
          });

    } catch (InvalidBloggerPostException e) {
      // Rethrow as more intelligible exception
      throw new InvalidBloggerFeedException("Error parsing blog entries", e);
    }

    // Return a Stream of BloggerPosts
    return localEntries.stream();
  }

  // Overridden methods
  @NotNull
  @Override
  public String toString() {
    return "[\n"
        + "\tTitle: "
        + this.title
        + "\n\tVersion: "
        + this.version
        + "\n\tAuthor: "
        + this.author
        + "\n\tLink: "
        + this.link
        + "\n\tEntries: "
        + this.entries.count()
        + "\n]";
  }
}
