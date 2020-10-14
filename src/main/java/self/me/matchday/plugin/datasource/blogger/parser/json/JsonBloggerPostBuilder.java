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
import org.jetbrains.annotations.Nullable;
import self.me.matchday.plugin.datasource.blogger.BloggerPost;
import self.me.matchday.plugin.datasource.blogger.InvalidBloggerPostException;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerPostBuilder;
import self.me.matchday.util.Log;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Constructs a fully-formed BloggerPost object which is then passed to the BloggerPost
 * constructor.
 */
public class JsonBloggerPostBuilder implements BloggerPostBuilder {

    private static final String LOG_TAG = "JsonBloggerPostBuilder";

    private final JsonObject postJson;
    private final String bloggerPostID;
    private final String title;
    private final String content;
    private final String link;
    private final LocalDateTime published;
    private final LocalDateTime lastUpdated;
    private final List<String> categories = new ArrayList<>();

    public JsonBloggerPostBuilder(@NotNull final JsonObject postJson) {

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
            final String formattedPostId =
                    postJson
                            .get("id")
                            .getAsJsonObject()
                            .get("$t")
                            .getAsString();
            // Extract post ID
            final Matcher matcher = BloggerPostBuilder.postIdPattern.matcher(formattedPostId);
            if (matcher.find()) {
                return matcher.group(2);
            }
            // Default to complete String
            return formattedPostId;

        } catch (NullPointerException | IndexOutOfBoundsException e) {
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
