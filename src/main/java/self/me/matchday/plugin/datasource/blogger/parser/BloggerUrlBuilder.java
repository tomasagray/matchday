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

package self.me.matchday.plugin.datasource.blogger.parser;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.util.UriUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BloggerUrlBuilder {

  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  protected final String baseUrl;
  protected LocalDateTime endDate;
  protected LocalDateTime startDate;
  protected boolean fetchBodies;
  protected boolean fetchImages;
  protected List<String> labels;
  protected int maxResults;
  protected String orderBy;
  protected String pageToken;
  protected String status;

  protected BloggerUrlBuilder(@NotNull final String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public BloggerUrlBuilder endDate(LocalDateTime endDate) {
    this.endDate = endDate;
    return this;
  }

  public BloggerUrlBuilder startDate(LocalDateTime startDate) {
    this.startDate = startDate;
    return this;
  }

  public BloggerUrlBuilder fetchBodies(boolean fetchBodies) {
    this.fetchBodies = fetchBodies;
    return this;
  }

  public BloggerUrlBuilder fetchImages(boolean fetchImages) {
    this.fetchImages = fetchImages;
    return this;
  }

  public BloggerUrlBuilder labels(List<String> labels) {
    this.labels = labels;
    return this;
  }

  public BloggerUrlBuilder maxResults(int maxResults) {
    this.maxResults = maxResults;
    return this;
  }

  public BloggerUrlBuilder orderBy(String orderBy) {
    this.orderBy = orderBy;
    return this;
  }

  public BloggerUrlBuilder pageToken(String pageToken) {
    this.pageToken = pageToken;
    return this;
  }

  public BloggerUrlBuilder status(String status) {
    this.status = status;
    return this;
  }

  public abstract URL buildUrl() throws MalformedURLException;

  protected @NotNull String getLabelQuery(@NotNull final String labelPattern) {
    // Concat labels
    if (labels == null) {
      return "";
    } else {
      final String concatLabels =
          labels
              .stream()
              .map(label -> UriUtils.encode(label, StandardCharsets.UTF_8))
              .collect(Collectors.joining("/"));
      return
          String.format(labelPattern, concatLabels);
    }
  }

}
