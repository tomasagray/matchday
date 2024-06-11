/*
 * Copyright (c) 2022.
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

package net.tomasbot.matchday.plugin.datasource.blogger;

import static net.tomasbot.matchday.plugin.datasource.blogger.model.BloggerFeed.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import net.tomasbot.matchday.plugin.datasource.blogger.model.BloggerFeed;

class BloggerEntryParser {

  private static final Pattern PUB_PATTERN = Pattern.compile("\\d{2}/\\d{2}/\\d{2,4}");
  private static final DateTimeFormatter PUB_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final Element element;

  BloggerEntryParser(@NotNull final Element element) {
    this.element = element;
  }

  @Nullable
  private static Link getLink(String _url) {
    try {
      final URL url = new URL(_url);
      final Link link = new Link();
      link.setHref(url);
      link.setRel("self");
      return link;
    } catch (MalformedURLException ignored) {
    }
    return null;
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
                return Generic.of(meta.attr("content"));
              }
              return null;
            })
        .orElse(null);
  }

  BloggerFeed.Generic<LocalDateTime> getPublished() {
    final String text = element.text();
    final Matcher matcher = PUB_PATTERN.matcher(text);
    if (matcher.find()) {
      final String data = matcher.group();
      final LocalDateTime published = LocalDate.parse(data, PUB_FORMATTER).atStartOfDay();
      return Generic.of(published);
    }
    return null;
  }

  Str getTitle() {
    return element.select("h3.entry-title").stream()
        .findFirst()
        .map(Element::text)
        .map(Str::new)
        .orElse(null);
  }

  Str getContent() {
    return element.select("div.entry-content").stream()
        .findFirst()
        .map(Element::toString)
        .map(Str::new)
        .orElse(null);
  }

  List<Link> getLinks() {
    final Evaluator.AttributeWithValue eval = new Evaluator.AttributeWithValue("itemprop", "url");
    return element.select("meta").stream()
        .filter(elem -> eval.matches(element, elem))
        .map(elem -> elem.attr("content"))
        .map(BloggerEntryParser::getLink)
        .collect(Collectors.toList());
  }

  List<Author> getAuthors() {
    final Evaluator.AttributeWithValue eval =
        new Evaluator.AttributeWithValue("itemprop", "author");
    return element.select("span").stream()
        .filter(elem -> eval.matches(element, elem))
        .map(Element::text)
        .map(
            auth -> {
              final Author author = new Author();
              author.setName(Generic.of(auth));
              return author;
            })
        .collect(Collectors.toList());
  }

  List<Term> getTerms() {
    return element.select("span.post-labels > a").stream()
        .map(Element::text)
        .map(
            t -> {
              final Term term = new Term();
              term.setTerm(t);
              return term;
            })
        .collect(Collectors.toList());
  }
}
