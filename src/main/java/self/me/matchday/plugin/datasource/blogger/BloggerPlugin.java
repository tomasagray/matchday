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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import self.me.matchday.db.DataSourceRepository;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Event;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.DataSourcePlugin;
import self.me.matchday.plugin.datasource.blogger.model.Blogger;
import self.me.matchday.plugin.datasource.blogger.model.BloggerEntry;
import self.me.matchday.plugin.datasource.parsing.EntityParser;
import self.me.matchday.plugin.datasource.parsing.PatternKit;

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
      @NotNull EntityParser entityParser,
      @NotNull DataSourceRepository dataSourceRepo,
      @NotNull BloggerPluginProperties pluginProperties,
      @NotNull QueryBuilderService queryBuilder,
      @NotNull DataSourceValidator dataSourceValidator) {

    this.entityParser = entityParser;
    this.dataSourceRepo = dataSourceRepo;
    this.pluginProperties = pluginProperties;
    this.queryBuilder = queryBuilder;
    this.dataSourceValidator = dataSourceValidator;
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
  public void validateDataSource(@NotNull DataSource dataSource) {
    dataSourceValidator.validateDataSourcePluginId(this.getPluginId(), dataSource.getPluginId());
    dataSourceValidator.validateDataSourceUri(dataSource.getBaseUri());
  }

  @Override
  public Snapshot<? extends Event> getAllSnapshots(@NotNull SnapshotRequest request) {

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
  public Snapshot<? extends Event> getSnapshot(
      @NotNull SnapshotRequest request, @NotNull DataSource dataSource) throws IOException {

    final Stream<? extends Event> events = getEventStream(request, dataSource);
    return Snapshot.of(events);
  }

  @NotNull
  private Stream<? extends Event> getEventStream(
      @NotNull SnapshotRequest request, @NotNull DataSource dataSource) throws IOException {

    validateDataSource(dataSource);

    // determine whether HTML or JSON parser is called for
    final SourceType type = getSourceType(dataSource.getBaseUri());
    final BloggerParser parser =
        type == SourceType.JSON ? new JsonBloggerParser() : new HtmlBloggerParser();
    // parse request into blogger query
    final String query = queryBuilder.buildQueryFrom(request, type);
    final URL bloggerUrl = dataSource.getBaseUri().resolve(query).toURL();
    // use appropriate BloggerParser to get snapshot
    final Blogger blogger = parser.getBlogger(bloggerUrl);

    // parse events from Blogger instance
    final List<PatternKit<? extends Event>> patternKits = List.of(new PatternKit<>(Event.class));
    // todo -                   get real pattern kits from repo/service ^
    return parseEvents(blogger, patternKits);
  }

  private @NotNull Stream<? extends Event> parseEvents(
      @NotNull Blogger blogger, @NotNull List<PatternKit<? extends Event>> patternKits) {

    return blogger.getFeed().getEntry().stream()
        .flatMap(entry -> parseEntry(patternKits, entry))
        .filter(Objects::nonNull);
  }

  private Stream<? extends Event> parseEntry(
      @NotNull List<PatternKit<? extends Event>> patternKits, @NotNull BloggerEntry entry) {

    final String content = entry.getContent().getData();

    // parse content - text, links, etc.
    final Document document = Jsoup.parse(content);
    final String text = document.text();
    // get event data - plain text - event pattern kit
    // get file source data - plain text - file source pattern kit
    // get links - HTML - video file pattern kit
    //    document.select()
    // zip streams together

    return null; // entityParser.createEntityStream(patternKits, content);
  }
}
