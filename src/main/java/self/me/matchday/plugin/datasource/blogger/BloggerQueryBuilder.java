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

package self.me.matchday.plugin.datasource.blogger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.util.UriEncoder;
import self.me.matchday.model.SnapshotRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BloggerQueryBuilder {

  private final SnapshotRequest request;

  BloggerQueryBuilder(@NotNull final SnapshotRequest request) {
    this.request = request;
  }

  @NotNull
  String build() {

    final String queryPrefix = getQueryUrlPrefix();
    final String labelsQuery = getLabelsQuery();
    final String params = getParams();
    String query = labelsQuery + (!params.equals("") ? "?" + params : "");
    if (!"".equals(query)) {
      query = queryPrefix + query;
    }
    return query;
  }

  abstract String getQueryUrlPrefix();

  abstract String getLabelPrefix();

  abstract String getParams();

  protected String getLabelsQuery() {

    String labelsQuery = "";
    final List<String> labels = request.getLabels();
    if (labels != null) {
      final String allLabels =
          labels.stream().map(UriEncoder::encode).collect(Collectors.joining("/"));
      if (!allLabels.equals("")) {
        final String labelPrefix = getLabelPrefix();
        labelsQuery = labelPrefix + allLabels;
      }
    }
    return labelsQuery;
  }

  @Nullable
  protected String getUpdatedMax() {

    final LocalDateTime endDate = request.getEndDate();
    String updatedMax = null;
    if (endDate != null) {
      updatedMax =
          String.format(
              "updated-max=%s&orderBy=updated", endDate.format(DateTimeFormatter.ISO_DATE_TIME));
    }
    return updatedMax;
  }

  @Nullable
  protected String getUpdatedMin() {

    final LocalDateTime startDate = request.getStartDate();
    String updatedMin = null;
    if (startDate != null) {
      updatedMin =
          String.format(
              "updated-min=%s&orderBy=updated", startDate.format(DateTimeFormatter.ISO_DATE_TIME));
    }
    return updatedMin;
  }

  @Nullable
  protected String getMaxResults() {

    final int maxResults = request.getMaxResults();
    String maxResultQuery = null;
    if (maxResults > 0) {
      maxResultQuery = String.format("max-results=%d", maxResults);
    }
    return maxResultQuery;
  }

  public static class HtmlQueryBuilder extends BloggerQueryBuilder {

    HtmlQueryBuilder(@NotNull SnapshotRequest request) {
      super(request);
    }

    @Override
    String getQueryUrlPrefix() {
      return "/search";
    }

    @Override
    String getLabelPrefix() {
      return "/label/";
    }

    @NotNull
    protected String getParams() {

      // todo - other query params?
      final String maxResults = getMaxResults();
      final String updatedMin = getUpdatedMin();
      final String updatedMax = getUpdatedMax();
      return Stream.of(maxResults, updatedMax, updatedMin)
          .filter(s -> s != null && !s.equals(""))
          .collect(Collectors.joining("&"));
    }
  }

  public static class JsonQueryBuilder extends BloggerQueryBuilder {

    JsonQueryBuilder(@NotNull SnapshotRequest request) {
      super(request);
    }

    @Override
    String getQueryUrlPrefix() {
      return "";
    }

    @Override
    String getLabelPrefix() {
      return "/-/";
    }

    @Override
    String getParams() {

      final String json = "alt=json";
      final String maxResults = getMaxResults();
      final String updatedMin = getUpdatedMin();
      final String updatedMax = getUpdatedMax();
      return Stream.of(json, maxResults, updatedMax, updatedMin)
          .filter(s -> s != null && !s.equals(""))
          .collect(Collectors.joining("&"));
    }
  }
}
