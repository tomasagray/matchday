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
import org.springframework.stereotype.Component;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.DataSourcePlugin;
import self.me.matchday.plugin.datasource.blogger.model.Blogger;
import self.me.matchday.plugin.datasource.blogger.model.BloggerEntry;
import self.me.matchday.plugin.datasource.blogger.model.BloggerFeed;
import self.me.matchday.plugin.datasource.parsing.HypertextEntityParser;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class BloggerPlugin implements DataSourcePlugin {

  // external dependencies
  private final HypertextEntityParser entityParser;
  // internal dependencies
  private final BloggerPluginProperties pluginProperties;
  private final QueryBuilderService queryBuilder;
  private final DataSourceValidator dataSourceValidator;

  BloggerPlugin(
      @NotNull HypertextEntityParser entityParser,
      @NotNull BloggerPluginProperties pluginProperties,
      @NotNull QueryBuilderService queryBuilder,
      @NotNull DataSourceValidator dataSourceValidator) {

    this.entityParser = entityParser;
    this.pluginProperties = pluginProperties;
    this.queryBuilder = queryBuilder;
    this.dataSourceValidator = dataSourceValidator;
  }

  @NotNull
  private SourceType getSourceType(@NotNull URI baseUri) {

    final Pattern jsonUrlPattern = pluginProperties.getJsonUrlPattern();
    final Matcher jsonUrlMatcher = jsonUrlPattern.matcher(baseUri.toString());
    return jsonUrlMatcher.find() ? SourceType.JSON : SourceType.HTML;
  }

  @Override
  public UUID getPluginId() {
    return UUID.fromString(pluginProperties.getId());
  }

  @Override
  public String getTitle() {
    return pluginProperties.getTitle();
  }

  @Override
  public String getDescription() {
    return pluginProperties.getDescription();
  }

  @Override
  public void validateDataSource(@NotNull DataSource<?> dataSource) {
    dataSourceValidator.validateDataSourcePluginId(this.getPluginId(), dataSource.getPluginId());
    dataSourceValidator.validateDataSourceUri(dataSource.getBaseUri());
    dataSourceValidator.validateDataSourceType(dataSource);
  }

  @Override
  public <T> Snapshot<T> getSnapshot(
      @NotNull SnapshotRequest request, @NotNull DataSource<T> dataSource) throws IOException {

    validateDataSource(dataSource);

    final URI baseUri = dataSource.getBaseUri();
    final Stream<String> hypertextStream = getHypertextStream(request, baseUri);
    final Stream<T> entityStream =
        hypertextStream.flatMap(data -> entityParser.getEntityStream(dataSource, data));
    return Snapshot.of(entityStream);
  }

  @NotNull
  private Stream<String> getHypertextStream(@NotNull SnapshotRequest request, @NotNull URI baseUri)
      throws IOException {

    // determine whether HTML or JSON parser is called for
    final SourceType type = getSourceType(baseUri);
    final BloggerParser parser =
        type == SourceType.JSON ? new JsonBloggerParser() : new HtmlBloggerParser();
    // parse request into blogger query
    final String query = queryBuilder.buildQueryFrom(request, type);
    final URL queryUrl = baseUri.resolve(query).toURL();
    // use appropriate BloggerParser to get snapshot
    final Blogger blogger = parser.getBlogger(queryUrl);

    return blogger.getFeed().getEntry().stream()
        .map(BloggerEntry::getContent)
        .map(BloggerFeed.Str::getData);
  }

  enum SourceType {
    HTML,
    JSON,
  }
}
