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

package self.me.matchday.plugin.datasource.blogger.model;

import lombok.*;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class BloggerFeed {

  @Id @GeneratedValue private Long feedId;

  private URL xmlns;
  private URL xmlns$openSearch;
  private URL xmlns$blogger;
  private URL xmlns$georss;
  private URL xmlns$gd;
  private URL xmlns$thr;
  private Generic<String> id;
  private Generic<LocalDateTime> updated;
  private List<Term> category;
  private Str title;
  private Str subtitle;
  private List<Link> link;
  private List<Author> author;
  private Generator generator;
  private Generic<Integer> openSearch$totalResults;
  private Generic<Integer> openSearch$startIndex;
  private Generic<Integer> openSearch$itemsPerPage;
  private List<BloggerEntry> entry;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    BloggerFeed that = (BloggerFeed) o;
    return Objects.equals(feedId, that.feedId);
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public static class Generic<T> {

    public T $t;

    public static <T> @NotNull Generic<T> of(T t) {
      final Generic<T> gen = new Generic<>();
      gen.$t = t;
      return gen;
    }

    public String toString() {
      if ($t != null) {
        return $t.toString();
      }
      return null;
    }
  }

  @Data
  public static class Str {
    private String type;
    private String $t;

    public Str(final String t) {
      $t = t;
    }

    public String getData() {
      return this.$t;
    }
  }

  @Data
  public static class Term {
    private String term;
    private URL scheme;
  }

  @Data
  public static class Link {
    private String rel;
    private String type;
    private URL href;
  }

  @Data
  public static class Image {
    private URL rel;
    private Integer width;
    private Integer height;
    private URL src;
  }

  @Data
  public static class Author {
    private Generic<String> name;
    private Generic<URI> uri;
    private Generic<String> email;
    private Image gd$image;
  }

  @Data
  public static class Generator {
    private String version;
    private URI uri;
    private String $t;

    public Generator(final String t) {
      this.$t = t;
    }
  }
}
