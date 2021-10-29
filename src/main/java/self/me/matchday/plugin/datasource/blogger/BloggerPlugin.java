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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import self.me.matchday.db.DataSourceRepository;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Event;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.model.video.VideoSourceMetadataPatternKit;
import self.me.matchday.plugin.datasource.DataSourcePlugin;
import self.me.matchday.plugin.datasource.EntryParser;
import self.me.matchday.plugin.datasource.blogger.model.Blogger;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class BloggerPlugin implements DataSourcePlugin<Event> {

  private final DataSourceRepository dataSourceRepo;
  private final BloggerPluginProperties pluginProperties;
  private final BloggerQueryBuilder queryBuilder;

  @Value("${plugin.blogger.blogger-url-pattern}")
  private String bloggerUrlPattern;

  BloggerPlugin(
      @NotNull DataSourceRepository dataSourceRepo,
      @NotNull BloggerPluginProperties pluginProperties,
      @NotNull BloggerQueryBuilder queryBuilder) {

    this.dataSourceRepo = dataSourceRepo;
    this.pluginProperties = pluginProperties;
    this.queryBuilder = queryBuilder;
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
  public DataSource addDataSource(
      @NotNull URI baseUri, @NotNull List<VideoSourceMetadataPatternKit> metadataPatterns) {

    validateDataSourceUri(baseUri);

    final BloggerDataSource dataSource =
        new BloggerDataSource(baseUri, metadataPatterns, this.getPluginId());
    // determine parser type
    final Matcher jsonUrlMatcher = pluginProperties.getJsonUrlPattern().matcher(baseUri.toString());
    final BloggerDataSource.SourceType type =
        jsonUrlMatcher.find()
            ? BloggerDataSource.SourceType.JSON
            : BloggerDataSource.SourceType.HTML;
    dataSource.setSourceType(type);
    return dataSourceRepo.saveAndFlush(dataSource);
  }

  private void validateDataSourceUri(@NotNull URI uri) {

    final Pattern urlPattern = Pattern.compile(bloggerUrlPattern);
    final Matcher matcher = urlPattern.matcher(uri.toString());
    if (!matcher.find()) {
      throw new IllegalArgumentException("Given URL is not a Blogger URL: " + uri);
    }
  }

  @Override
  public Snapshot<Event> getAllSnapshots(@NotNull SnapshotRequest request) {

    // get all DataSources for this plugin
    final List<DataSource> dataSources =
        dataSourceRepo.findDataSourcesByPluginId(this.getPluginId());
    // snapshot all DataSources & collate
    final Stream<Event> events =
        dataSources.stream()
            .flatMap(
                source -> {
                  try {
                    return getEventStream(request, source);
                  } catch (IOException wrapped) {
                    throw new RuntimeException(wrapped);
                  }
                });
    return Snapshot.of(events);
  }

  @Override
  public Snapshot<Event> getSnapshot(
      @NotNull SnapshotRequest request, @NotNull DataSource dataSource) throws IOException {

    final Stream<Event> events = getEventStream(request, dataSource);
    return Snapshot.of(events);
  }

  @NotNull
  private Stream<Event> getEventStream(
      @NotNull SnapshotRequest request, @NotNull DataSource dataSource) throws IOException {

    // ensure dataSource is a BloggerDataSource, cast
    if (!(dataSource instanceof BloggerDataSource)) {
      throw new IllegalArgumentException("Not a Blogger data source: " + dataSource);
    }
    final BloggerDataSource bloggerDataSource = (BloggerDataSource) dataSource;

    // determine whether HTML or JSON parser is called for
    final BloggerParser parser =
        bloggerDataSource.getSourceType() == BloggerDataSource.SourceType.JSON
            ? new JsonBloggerParser()
            : new HtmlBloggerParser();
    // parse request into blogger query
    final String query = queryBuilder.buildQueryFrom(request);
    final URL bloggerUrl = dataSource.getBaseUri().resolve(query).toURL();
    // use appropriate BloggerParser to get snapshot
    final Blogger blogger = parser.getBlogger(bloggerUrl);

    // parse events from Blogger instance
    return parseEvents(blogger, dataSource.getMetadataPatterns());
  }

  private @NotNull Stream<Event> parseEvents(
      @NotNull Blogger blogger, @NotNull List<VideoSourceMetadataPatternKit> metadataPatterns) {

    return blogger.getFeed().getEntry().stream()
        .flatMap(
            entry -> {
              final String content = entry.getContent().getData();
              return EntryParser.parse(content).with(metadataPatterns);
            })
        .filter(Objects::nonNull);
  }
}
