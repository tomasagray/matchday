/*
 * Copyright (c) 2021.
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

package self.me.matchday.plugin.datasource.blogger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import self.me.matchday.plugin.datasource.blogger.model.Blogger;
import self.me.matchday.plugin.datasource.blogger.model.BloggerEntry;
import self.me.matchday.plugin.datasource.blogger.model.BloggerFeed;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HtmlBloggerParser implements BloggerParser {

  @Override
  public Blogger getBlogger(@NotNull String data) {
    final Document html = Jsoup.parse(data);
    return parseBlogger(html);
  }

  private @NotNull Blogger parseBlogger(@NotNull final Document html) {

    final Blogger blogger = new Blogger();
    // parse version
    final Element header = html.select("div#Header1").first();
    if (header != null) {
      blogger.setVersion(header.attr("data-version"));
    }
    final String encoding = parseEncoding(html);
    final BloggerFeed feed = parseFeed(html);

    blogger.setEncoding(encoding);
    blogger.setFeed(feed);
    return blogger;
  }

  private String parseEncoding(@NotNull Document html) {
    final Pattern encodingPattern = Pattern.compile("charset=([\\w-]+)");
    final Elements metas = html.select("meta");
    return metas.stream()
        .filter(
            meta ->
                new Evaluator.AttributeWithValue("http-equiv", "Content-Type").matches(html, meta))
        .findFirst()
        .map(
            elem -> {
              final String encoding = elem.attr("content");
              final Matcher matcher = encodingPattern.matcher(encoding);
              if (matcher.find()) {
                return matcher.group(1);
              }
              return null;
            })
        .orElse(null);
  }

  private BloggerFeed parseFeed(@NotNull final Document html) {

    final BloggerFeedParser feedParser = new BloggerFeedParser(html);
    return BloggerFeed.builder()
        .xmlns(feedParser.getXmlNs())
        .id(feedParser.getId())
        .category(feedParser.getCategory())
        .title(new BloggerFeed.Str(feedParser.getMetadata("og:Str")))
        .subtitle(new BloggerFeed.Str(feedParser.getMetadata("og:description")))
        .link(feedParser.getLinks())
        .generator(feedParser.getGenerator())
        .entry(feedParser.getEntries())
        .build();
  }

  static class BloggerFeedParser {

    private static final Pattern IdPattern = Pattern.compile("'blogId': '(\\d+)'");
    private static final Pattern TermPattern =
        Pattern.compile("^http[s]?://[\\w.:/]*/search/label/([\\w%-.])+");

    private final Document html;

    BloggerFeedParser(@NotNull final Document html) {
      this.html = html;
    }

    @Nullable
    private static BloggerFeed.Link getSelfLink(@NotNull final Element elem) {
      final String rel = elem.attr("rel");
      switch (rel) {
        case "canonical":
        case "alternate":
        case "service.post":
          try {
            final BloggerFeed.Link link = new BloggerFeed.Link();
            link.setHref(new URL(elem.attr("href")));
            link.setRel(rel);
            link.setType(elem.attr("type"));
            return link;
          } catch (MalformedURLException ignored) {
          }
      }
      return null;
    }

    URL getXmlNs() {
      try {
        final Element tag = this.html.selectFirst("html");
        if (tag != null) {
          return new URL(tag.attr("xmlns"));
        }
      } catch (MalformedURLException ignored) {
      }
      return null;
    }

    BloggerFeed.Generic<String> getId() {
      final Elements endScript = html.select("script:last-of-type");
      final String script = endScript.text();
      final Matcher idMatcher = IdPattern.matcher(script);
      if (idMatcher.find()) {
        return BloggerFeed.Generic.of(idMatcher.group(1));
      }
      return null;
    }

    List<BloggerFeed.Term> getCategory() {
      final List<BloggerFeed.Term> terms = new ArrayList<>();
      final Elements links = html.select("a");
      links.forEach(
          link -> {
            final Matcher matcher = TermPattern.matcher(link.attr("href"));
            if (matcher.find()) {
              final BloggerFeed.Term term = new BloggerFeed.Term();
              term.setTerm(link.text());
              terms.add(term);
            }
          });
      return terms;
    }

    String getMetadata(@NotNull final String property) {

      final Evaluator.AttributeWithValue eval =
          new Evaluator.AttributeWithValue("property", property);
      final Elements metas = html.select("meta");
      return metas.stream()
          .filter(meta -> eval.matches(html, meta))
          .map(meta -> meta.attr("content"))
          .findFirst()
          .orElse(null);
    }

    List<BloggerFeed.Link> getLinks() {
      final List<BloggerFeed.Link> links = new ArrayList<>();
      final Elements elements = html.select("link");
      // add self links
      elements.forEach(
          elem -> {
            final BloggerFeed.Link link = getSelfLink(elem);
            if (link != null) {
              links.add(link);
            }
          });
      // add 'next' link
      final BloggerFeed.Link nextLink = getNextLink();
      if (nextLink != null) {
        links.add(nextLink);
      }
      return links;
    }

    @Nullable
    private BloggerFeed.Link getNextLink() {
      try {
        final Element nextLink = html.selectFirst("a.blog-pager-older-link");
        if (nextLink != null) {
          final BloggerFeed.Link link = new BloggerFeed.Link();
          link.setHref(new URL(nextLink.attr("href")));
          link.setRel("next");
          link.setType("application/atom+xml");
          return link;
        }
      } catch (MalformedURLException ignored) {
      }
      return null;
    }

    BloggerFeed.Generator getGenerator() {
      final Elements elements = html.select("meta");
      final Evaluator.AttributeWithValue eval =
          new Evaluator.AttributeWithValue("name", "generator");
      return elements.stream()
          .findFirst()
          .map(
              elem -> {
                if (eval.matches(html, elem)) {
                  return new BloggerFeed.Generator(elem.attr("content"));
                }
                return null;
              })
          .orElse(null);
    }

    List<BloggerEntry> getEntries() {
      return html.select("div.post-outer").stream()
          .map(
              entry -> {
                final BloggerEntryParser parser = new BloggerEntryParser(entry);
                return BloggerEntry.builder()
                    .id(parser.getId())
                    .published(parser.getPublished())
                    .category(parser.getTerms())
                    .title(parser.getStr())
                    .content(parser.getContent())
                    .link(parser.getLinks())
                    .author(parser.getAuthors())
                    .build();
              })
          .collect(Collectors.toList());
    }
  }

  static class BloggerEntryParser {

    private final Element element;

    BloggerEntryParser(@NotNull final Element element) {
      this.element = element;
    }

    BloggerFeed.Generic<String> getId() {
      final Evaluator.AttributeWithValue eval =
          new Evaluator.AttributeWithValue("itemprop", "postId");
      final Elements metas = element.select("meta");
      return metas.stream()
          .findFirst()
          .map(
              meta -> {
                if (eval.matches(element, meta)) {
                  return BloggerFeed.Generic.of(meta.attr("content"));
                }
                return null;
              })
          .orElse(null);
    }

    BloggerFeed.Generic<LocalDateTime> getPublished() {
      return element.select("abbr.published").stream()
          .findFirst()
          .map(
              elem -> {
                final String title = elem.attr("title");
                final LocalDateTime published =
                    LocalDateTime.parse(title, DateTimeFormatter.ofPattern(DATETIME_PATTERN));
                return BloggerFeed.Generic.of(published);
              })
          .orElse(null);
    }

    BloggerFeed.Str getStr() {
      return element.select("h3.entry-Str").stream()
          .findFirst()
          .map(Element::text)
          .map(BloggerFeed.Str::new)
          .orElse(null);
    }

    BloggerFeed.Str getContent() {
      return element.select("div.entry-content > div").stream()
          .findFirst()
          .map(Element::toString)
          .map(BloggerFeed.Str::new)
          .orElse(null);
    }

    List<BloggerFeed.Link> getLinks() {
      final Evaluator.AttributeWithValue eval = new Evaluator.AttributeWithValue("itemprop", "url");
      return element.select("meta").stream()
          .filter(elem -> eval.matches(element, elem))
          .map(elem -> elem.attr("content"))
          .map(
              _url -> {
                try {
                  final URL url = new URL(_url);
                  final BloggerFeed.Link link = new BloggerFeed.Link();
                  link.setHref(url);
                  link.setRel("self");
                  return link;
                } catch (MalformedURLException ignored) {
                }
                return null;
              })
          .collect(Collectors.toList());
    }

    List<BloggerFeed.Author> getAuthors() {
      final Evaluator.AttributeWithValue eval =
          new Evaluator.AttributeWithValue("itemprop", "author");
      return element.select("span").stream()
          .filter(elem -> eval.matches(element, elem))
          .map(Element::text)
          .map(
              auth -> {
                final BloggerFeed.Author author = new BloggerFeed.Author();
                author.setName(BloggerFeed.Generic.of(auth));
                return author;
              })
          .collect(Collectors.toList());
    }

    List<BloggerFeed.Term> getTerms() {
      return element.select("span.post-labels > a").stream()
          .map(Element::text)
          .map(
              t -> {
                final BloggerFeed.Term term = new BloggerFeed.Term();
                term.setTerm(t);
                return term;
              })
          .collect(Collectors.toList());
    }
  }
}
