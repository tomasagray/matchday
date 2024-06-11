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

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import net.tomasbot.matchday.model.DataSource;
import net.tomasbot.matchday.model.Snapshot;
import net.tomasbot.matchday.model.SnapshotRequest;
import net.tomasbot.matchday.plugin.datasource.DataSourcePlugin;
import net.tomasbot.matchday.plugin.datasource.blogger.model.Blogger;
import net.tomasbot.matchday.plugin.datasource.blogger.model.BloggerEntry;
import net.tomasbot.matchday.plugin.datasource.blogger.model.BloggerFeed;
import net.tomasbot.matchday.plugin.datasource.parsing.HypertextEntityParser;

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

  private static void correctPublished(@NotNull BloggerEntry entry) {
    if (entry.getPublished() == null) {
      BloggerFeed.Generic<LocalDateTime> current = BloggerFeed.Generic.of(LocalDateTime.now());
      entry.setPublished(current);
    }
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

  @Override
  public <T> Snapshot<T> getUrlSnapshot(@NotNull URL url, @NotNull DataSource<T> dataSource)
      throws IOException {

    final List<BloggerEntry> entries = getEntriesUntil(new HtmlBloggerParser(), url, null);
    Stream<String> feed =
        entries.stream().map(BloggerEntry::getContent).map(BloggerFeed.Str::getData);
    Stream<T> entities = feed.flatMap(data -> entityParser.getEntityStream(dataSource, data));
    return Snapshot.of(entities);
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
    URL queryUrl = baseUri.resolve(query).toURL();
    final LocalDateTime target = request.getStartDate();
    final List<BloggerEntry> entries = getEntriesUntil(parser, queryUrl, target);
    return entries.stream().map(BloggerEntry::getContent).map(BloggerFeed.Str::getData);
  }

  /**
   * Recursively scan Blogger entries until the specified date is reached
   *
   * @param parser The Blogger parser
   * @param queryUrl Beginning query URL
   * @param target The date a which to stop scanning
   * @return All BloggerEntries found during the scan
   * @throws IOException If the Blogger cannot be parsed
   */
  private @NotNull List<BloggerEntry> getEntriesUntil(
      @NotNull BloggerParser parser, @NotNull URL queryUrl, LocalDateTime target)
      throws IOException {
    final int MAX_SCAN_STEPS = pluginProperties.getMaxScanSteps();

    final List<BloggerEntry> allEntries = new ArrayList<>();
    URL _url = queryUrl;
    int steps = 0;
    do {
      steps++;
      final Blogger blogger = parser.getBlogger(_url);
      final BloggerFeed feed = blogger.getFeed();
      final List<BloggerEntry> entries = feed.getEntry();
      if (!entries.isEmpty()) {
        allEntries.addAll(entries);
        _url = getNextUrl(feed, entries, target);
      } else {
        _url = null;
      }
    } while (_url != null && steps < MAX_SCAN_STEPS);
    return allEntries;
  }

  @Nullable
  private URL getNextUrl(
      @NotNull BloggerFeed feed, @NotNull List<BloggerEntry> entries, LocalDateTime target) {
    if (target != null) {
      List<BloggerEntry> sorted =
          new ArrayList<>(entries)
              .stream()
                  .peek(BloggerPlugin::correctPublished)
                  .sorted(Comparator.comparing(e -> e.getPublished().$t))
                  .toList();
      final BloggerEntry leastRecent = sorted.get(0);
      final LocalDateTime current = leastRecent.getPublished().$t;
      if (current != null && !current.isBefore(target)) {
        BloggerFeed.Link next = feed.getNext();
        if (next != null) {
          return next.getHref();
        }
      }
    }
    return null;
  }

  public enum SourceType {
    HTML,
    JSON,
  }
}
