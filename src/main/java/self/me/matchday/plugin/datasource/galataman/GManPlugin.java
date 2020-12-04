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

package self.me.matchday.plugin.datasource.galataman;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.DataSourcePlugin;
import self.me.matchday.plugin.datasource.blogger.Blogger;
import self.me.matchday.plugin.datasource.blogger.BloggerPlugin;
import self.me.matchday.plugin.datasource.blogger.BloggerPlugin.FetchMode;
import self.me.matchday.plugin.datasource.bloggerparser.EventFileSourceParser;
import self.me.matchday.plugin.datasource.bloggerparser.EventMetadataParser;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class GManPlugin implements DataSourcePlugin<Stream<Event>> {

  private static final String LOG_TAG = "GManPlugin";

  private final BloggerPlugin bloggerPlugin;
  private final GmanPluginProperties pluginProperties;

  protected final EventMetadataParser eventMetadataParser;
  protected final EventFileSourceParser fileSourceParser;

  @Autowired
  public GManPlugin(
      final BloggerPlugin bloggerPlugin,
      final GmanPluginProperties pluginProperties,
      final GManPatterns gManPatterns,
      final EventMetadataParser eventMetadataParser,
      final GManEventFileSourceParser fileSourceParser) {

    this.eventMetadataParser = eventMetadataParser;
    this.fileSourceParser = fileSourceParser;
    this.eventMetadataParser.setBloggerParserPatterns(gManPatterns);

    this.pluginProperties = pluginProperties;
    this.bloggerPlugin = bloggerPlugin;
    // Configure Blogger plugin
    this.bloggerPlugin.setBaseUrl(this.pluginProperties.getBaseUrl());
    this.bloggerPlugin.setFetchMode(FetchMode.HTML);
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
  public @NotNull Snapshot<Stream<Event>> getSnapshot(@NotNull SnapshotRequest snapshotRequest)
      throws IOException {

    Log.i(
        LOG_TAG,
        String.format(
            "Refreshing Galataman HDF [@ %s] data with Snapshot:\n%s\n",
            pluginProperties.getBaseUrl(), snapshotRequest));

    return bloggerPlugin.getSnapshot(snapshotRequest).map(this::getEventStream);
  }

  private Stream<Event> getEventStream(@NotNull final Blogger blogger) {
    return blogger
        .getPosts()
        .map(
            bloggerPost -> {
              // Parse Event
              final Event event = eventMetadataParser.getEvent(bloggerPost.getTitle());
              // ParseEvent file sources & add to Event
              final List<EventFileSource> eventFileSources =
                  fileSourceParser.getEventFileSources(event, bloggerPost.getContent());
              event.addFileSources(eventFileSources);
              // Return completed Event
              return event;
            });
  }
}
