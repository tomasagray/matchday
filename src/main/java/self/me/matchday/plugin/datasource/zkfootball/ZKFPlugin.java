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

package self.me.matchday.plugin.datasource.zkfootball;

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
public class ZKFPlugin implements DataSourcePlugin<Stream<Event>> {

  private static final String LOG_TAG = "ZKFPlugin";

  private final BloggerPlugin bloggerPlugin;
  private final ZKFPluginProperties pluginProperties;

  protected final EventMetadataParser eventMetadataParser;
  protected final EventFileSourceParser fileSourceParser;

  @Autowired
  public ZKFPlugin(
      final BloggerPlugin bloggerPlugin,
      final ZKFPluginProperties pluginProperties,
      final ZKFPatterns zkfPatterns,
      final EventMetadataParser eventMetadataParser,
      final ZKFEventFileSourceParser fileSourceParser) {

    // Pass dependencies to super class
    //    super(zkfPatterns, eventMetadataParser, fileSourceParser);
    this.eventMetadataParser = eventMetadataParser;

    this.eventMetadataParser.setBloggerParserPatterns(zkfPatterns);
    this.fileSourceParser = fileSourceParser;
    this.pluginProperties = pluginProperties;

    this.bloggerPlugin = bloggerPlugin;
    // Setup Blogger plugin
    this.bloggerPlugin.setBaseUrl(pluginProperties.getBaseUrl());
    this.bloggerPlugin.setFetchMode(FetchMode.JSON);
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
            "Refreshing ZK Football [@ %s] data with Snapshot:\n%s\n",
            pluginProperties.getBaseUrl(), snapshotRequest));
    return bloggerPlugin.getSnapshot(snapshotRequest).map(this::getEventStream);
  }


  protected Stream<Event> getEventStream(@NotNull final Blogger blogger) {
    return blogger
            .getPosts()
            .map(
                    bloggerPost -> {
                      // Parse Event
                      final Event event = eventMetadataParser.getEvent(bloggerPost.getTitle());
                      // ParseEvent file sources & add to Event
                      final List<EventFileSource> eventFileSources =
                              fileSourceParser.getEventFileSources(bloggerPost.getContent());
                      event.addFileSources(eventFileSources);
                      // Return completed Event
                      return event;
                    });
  }
}
