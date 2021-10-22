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
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.util.UriEncoder;
import self.me.matchday.model.SnapshotRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class BloggerQueryBuilder {

  private final BloggerPluginProperties pluginProperties;

  BloggerQueryBuilder(BloggerPluginProperties pluginProperties) {
    this.pluginProperties = pluginProperties;
  }

  public @NotNull String buildQueryFrom(@NotNull final SnapshotRequest request) {

    // todo - other query params?
    final String labelsQuery = getLabelsQuery(request);
    final String maxResults = getMaxResults(request);
    final String updatedMin = getUpdatedMin(request);
    final String updatedMax = getUpdatedMax(request);

    String query =
        labelsQuery
            + Stream.of(maxResults, updatedMax, updatedMin)
                .filter(s -> s != null && !s.equals(""))
                .collect(Collectors.joining("&"));
    if (!"".equals(query)) {
      query = pluginProperties.getQueryUrlPrefix() + query;
    }
    return query;
  }

  @Nullable
  private String getUpdatedMax(@NotNull SnapshotRequest request) {

    final LocalDateTime endDate = request.getEndDate();
    String updatedMax = null;
    if (endDate != null) {
      updatedMax =
          String.format(
              "updated-max=%s&orderBy=updated",
              endDate.format(pluginProperties.getDateTimeFormatter()));
    }
    return updatedMax;
  }

  @Nullable
  private String getUpdatedMin(@NotNull SnapshotRequest request) {

    final LocalDateTime startDate = request.getStartDate();
    String updatedMin = null;
    if (startDate != null) {
      updatedMin =
          String.format(
              "updated-min=%s&orderBy=updated",
              startDate.format(pluginProperties.getDateTimeFormatter()));
    }
    return updatedMin;
  }

  @Nullable
  private String getMaxResults(@NotNull SnapshotRequest request) {

    final int maxResults = request.getMaxResults();
    String maxResultQuery = null;
    if (maxResults > 0) {
      maxResultQuery = String.format("max-results=%d", maxResults);
    }
    return maxResultQuery;
  }

  private String getLabelsQuery(@NotNull SnapshotRequest request) {

    String labelsQuery = "";
    final List<String> labels = request.getLabels();
    if (labels != null) {
      final String allLabels =
          labels.stream().map(UriEncoder::encode).collect(Collectors.joining("/"));
      if (!allLabels.equals("")) {
        labelsQuery = String.format("/label/%s", allLabels);
      }
    }
    return labelsQuery;
  }
}
