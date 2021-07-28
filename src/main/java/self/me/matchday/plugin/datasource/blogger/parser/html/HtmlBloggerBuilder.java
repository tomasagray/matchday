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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import self.me.matchday.plugin.datasource.blogger.Blogger;
import self.me.matchday.plugin.datasource.blogger.BloggerPost;
import self.me.matchday.plugin.datasource.blogger.InvalidBloggerFeedException;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerBuilder;
import self.me.matchday.util.URLQueryDecoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class HtmlBloggerBuilder implements BloggerBuilder {

  // Dependencies
  private final String data;
  private final HtmlPostBuilderFactory postBuilderFactory;

  private Document html;
  private long postCount;

  public HtmlBloggerBuilder(
      @NotNull final String data, @NotNull final HtmlPostBuilderFactory postBuilderFactory) {
    this.data = data;
    this.postBuilderFactory = postBuilderFactory;
  }

  private String parseBloggerId() {

    // Constants
    final String BLOG_ID_KEY = "targetBlogID";
    final String INVALID_BLOG_ID = "-1";

    // Get the <noscript> element of the <head>
    final Element link = this.html.select("head noscript link").first();
    // Parse URL
    final String href = link.attr("href");
    final Map<String, List<String>> query = URLQueryDecoder.decode(href);

    // Get the blog ID from the query string
    final List<String> strings = query.get(BLOG_ID_KEY);
    if (strings != null && strings.size() > 0) {
      return strings.get(0);
    } else {
      return INVALID_BLOG_ID;
    }
  }

  private @Nullable String parseTitle() {

    final Element title = this.html.select("title").first();
    return (title != null) ? title.text() : null;
  }

  private @Nullable String parseVersion() {

    // Select correct <div> element
    final Element header = this.html.select("div.Header").first();
    return (header != null) ? header.attr("data-version") : null;
  }

  private @Nullable String parseAuthor() {

    try {
      // Get the author <span> element
      final Element author = this.html.select("span[itemprop=author]").first();
      // Get the next <span> element which should contain the author data
      final Element name = author.nextElementSiblings().select("span[itemprop=name]").first();
      return name.text();
    } catch (NullPointerException e) {
      return null;
    }
  }

  private @Nullable String parseLink() {

    final Element metaUrl = this.html.select("meta[property=og:ICDUrl]").first();
    return (metaUrl != null) ? metaUrl.attr("content") : null;
  }

  private Stream<BloggerPost> parseEntries() {

    // Result container
    List<BloggerPost> entries = new ArrayList<>();
    final Elements posts = this.html.select("div.date-posts > div.post-outer");

    if (posts.isEmpty()) {
      throw new InvalidBloggerFeedException();
    }

    posts.forEach(
        element -> {
          // Create post parser
          final HtmlBloggerPostBuilder htmlPostBuilder =
              this.postBuilderFactory.createBuilder(element);
          // Parse the post & add to list
          entries.add(htmlPostBuilder.getBloggerPost());
          postCount++;
        });

    return entries.stream();
  }

  @Override
  public Blogger getBlogger() {

    // DOM-ify HTML String
    this.html = Jsoup.parse(data);
    // Get Blogger fields:
    String bloggerId = parseBloggerId();
    String title = parseTitle();
    String version = parseVersion();
    String author = parseAuthor();
    String link = parseLink();
    Stream<BloggerPost> posts = parseEntries();

    return new Blogger(bloggerId, title, version, author, link, posts, postCount);
  }
}
