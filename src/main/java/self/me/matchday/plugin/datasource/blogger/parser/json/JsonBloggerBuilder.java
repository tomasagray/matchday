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

package self.me.matchday.plugin.datasource.blogger.parser.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.io.JsonStreamReader;
import self.me.matchday.plugin.datasource.blogger.Blogger;
import self.me.matchday.plugin.datasource.blogger.BloggerPost;
import self.me.matchday.plugin.datasource.blogger.InvalidBloggerFeedException;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerBuilder;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import static self.me.matchday.plugin.datasource.blogger.parser.BloggerPostBuilder.bloggerIdPattern;

public class JsonBloggerBuilder implements BloggerBuilder {

    private final JsonPostBuilderFactory postBuilderFactory;
    private final URL url;

    private JsonObject feed;
    private long postCount;

    // Constructor
    public JsonBloggerBuilder(@NotNull final URL url,
                              @NotNull final JsonPostBuilderFactory postBuilderFactory) {

        // Save constructor args
        this.url = url;
        this.postBuilderFactory = postBuilderFactory;
    }

    /**
     * Get the "feed" portion of the JSON object. Also sets version field, as this is the only data
     * outside of the "plugin" structure.
     *
     * @param feed A JsonObject of the entire JSON plugin
     */
    private JsonObject parseFeed(@NotNull final JsonObject feed) {

        try {
            return
                    feed
                            .get("feed")
                            .getAsJsonObject();

        } catch (NullPointerException e) {
            throw new InvalidBloggerFeedException("Unable to get 'plugin' object", e);
        }
    }

    /**
     * Get the top-level Blogger ID.
     *
     * @return String representing the Blog's ID.
     */
    private String parseId() {

        try {
            String formattedId =
                    feed
                            .get("id")
                            .getAsJsonObject()
                            .get("$t")
                            .getAsString();
            final Matcher matcher = bloggerIdPattern.matcher(formattedId);
            if (matcher.find()) {
                return matcher.group(1);
            }
            // Default to complete ID
            return formattedId;

        } catch (NullPointerException e) {
            throw new InvalidBloggerFeedException("Could not parse blog ID", e);
        }
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
                            .feed
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
    private String parseVersion(@NotNull final JsonObject json) {

        // Attempt to parse version info
        try {
            return
                    json
                            .get("version")
                            .getAsString();
        } catch (NullPointerException e) {
            throw new InvalidBloggerFeedException("Could not parse blog version info", e);
        }
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
                            .feed
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
            JsonArray links = this.feed.get("link").getAsJsonArray();
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
        final JsonElement entry = this.feed.get("entry");
        // Ensure there is at least one entry
        if (entry == null || !(entry.isJsonArray())) {
            throw new InvalidBloggerFeedException(
                    String.format("Feed is empty from URL: %s", this.url));
        }

        // Iterate over blogEntries
        JsonArray blogPosts = entry.getAsJsonArray();
        blogPosts.forEach(
                (post) -> {
                    // Ensure valid JSON;
                    if (post.isJsonObject()) {
                        // Create a post parser
                        final JsonBloggerPostBuilder postBuilder =
                                postBuilderFactory.createPostBuilder(post.getAsJsonObject());
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
    public Blogger getBlogger() throws IOException {

        // Read the blog
        // todo - don't use this class
        final JsonObject json = JsonStreamReader.readRemote(url);
        this.feed = parseFeed(json);
        // Parse the blog metadata
        String version = parseVersion(json);
        String blogId = parseId();
        String title = parseTitle();
        String author = parseAuthor();
        String link = parseLink();

        // Parse blog entries; ensure the list is immutable
        Stream<BloggerPost> posts = parseEntries();

        return
                new Blogger(blogId, title, version, author, link, posts, postCount);
    }
}
