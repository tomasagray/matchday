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

package self.me.matchday.plugin.datasource.blogger.parser.html;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import self.me.matchday.plugin.datasource.blogger.BloggerPost;
import self.me.matchday.plugin.datasource.blogger.InvalidBloggerPostException;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerPostBuilder;

public class HtmlBloggerPostBuilder implements BloggerPostBuilder {

  private final Element html;
  // Fields
  private final String bloggerPostID;
  private final String title;
  private final String content;
  private final String link;
  private final LocalDateTime published;
  private final LocalDateTime lastUpdated;
  private final List<String> categories;

  HtmlBloggerPostBuilder(@NotNull final Element html) {

    this.html = html;
    // Parse data
    this.bloggerPostID = parsePostId();
    this.title = parseTitle();
    this.content = parseContent();
    this.link = parseLink();
    this.published = parsePublished();
    this.lastUpdated = parseLastUpdated();
    this.categories = parseCategories();
  }

  private String parsePostId() {

    // Get the post ID
    final Element metaTag = this.html.select("meta[itemprop=postId]").first();
    // Ensure we have an ID - cannot proceed otherwise!
    if (metaTag == null) {
      throw new InvalidBloggerPostException("Could not determine post ID");
    }
    return
        metaTag.attr("content");
  }

  private @Nullable String parseTitle() {

    final Element postTitle = this.html.select("h3.post-title").first();
    return
        (postTitle == null) ? null : postTitle.text();
  }

  private String parseContent() {

    final Element postBody = this.html.select("div.post-body").first();
    // Ensure we have content
    if (postBody == null) {
      throw new InvalidBloggerPostException("No post content");
    }

    return
        postBody.html();
  }

  private String parseLink() {

    // Get the post link from the post header
    final Element postLink = this.html.select("h3.post-title > a").first();
    // Ensure link was parsed
    if (postLink == null) {
      throw new InvalidBloggerPostException("Could not parse post link");
    }
    return
        postLink.attr("href");
  }

  private @Nullable LocalDateTime parsePublished() {

    final Element published = this.html.select("a.timestamp-link > abbr").first();
    return parseDateTime(published.attr("title"));
  }

  private LocalDateTime parseLastUpdated() {

    // Info unavailable in HTML data; default to same as published
    return this.published;
  }

  private List<String> parseCategories() {

    final Elements labels = this.html.select("span.post-labels > a");
    return
        labels
            .stream()
            .map(Element::text)
            .collect(Collectors.toList());
  }

  private @Nullable LocalDateTime parseDateTime(String published) {
    try {
      return
          LocalDateTime.parse(published);
    } catch (DateTimeParseException | NullPointerException e) {
      // Could not parse publish date
      return null;
    }
  }

  @Override
  public BloggerPost getBloggerPost() {
    return
        new BloggerPost(bloggerPostID, published, lastUpdated, categories, title, content, link);
  }
}
