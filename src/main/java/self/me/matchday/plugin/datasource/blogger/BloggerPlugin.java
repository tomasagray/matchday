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

package self.me.matchday.plugin.datasource.blogger;

import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import self.me.matchday.io.TextFileReader;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.DataSourcePlugin;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerBuilder;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerBuilderFactory;
import self.me.matchday.plugin.datasource.blogger.parser.html.HtmlBuilderFactory;
import self.me.matchday.plugin.datasource.blogger.parser.json.JsonBuilderFactory;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

@Data
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BloggerPlugin implements DataSourcePlugin<Blogger> {

  private static final String LOG_TAG = "BloggerPlugin";

  public enum FetchMode {
    JSON,
    HTML,
  }

  private final BloggerPluginProperties properties;
  private String baseUrl;
  private FetchMode fetchMode;
  private BloggerBuilderFactory bloggerBuilderFactory;

  public BloggerPlugin(@Autowired BloggerPluginProperties pluginProperties) {
    this.properties = pluginProperties;
  }

  /**
   * Supply the correct abstract factory implementation depending on the plugin setting.
   *
   * @param fetchMode What type of data is the plugin going to attempt to fetch?
   */
  public void setFetchMode(@NotNull final FetchMode fetchMode) {

    if (fetchMode == FetchMode.JSON) {
      bloggerBuilderFactory = new JsonBuilderFactory();
    } else if (fetchMode == FetchMode.HTML) {
      bloggerBuilderFactory = new HtmlBuilderFactory();
    }
  }

  @Override
  public UUID getPluginId() {
    return UUID.fromString(properties.getId());
  }

  @Override
  public String getTitle() {
    return properties.getTitle();
  }

  @Override
  public String getDescription() {
    return properties.getDescription();
  }

  /**
   * Get an instantaneous view (a Snapshot) of a Blogger blog.
   *
   * @param snapshotRequest The request data
   * @return A Snapshot of the Blogger
   * @throws IOException If the data cannot be read or parsed
   */
  @Override
  @Contract("_ -> new")
  public @NotNull Snapshot<Blogger> getSnapshot(@NotNull final SnapshotRequest snapshotRequest)
      throws IOException {

    // Get the required URL from the request data
    final URL url = getUrlFromRequest(snapshotRequest);
    Log.i(LOG_TAG, String.format("Getting Blogger snapshot from URL: %s", url));
    final String blogData = TextFileReader.readRemote(url);
    final BloggerBuilder bloggerBuilder = bloggerBuilderFactory.getBloggerBuilder(blogData);
    // Create a Blogger Snapshot & return
    return new Snapshot<>(bloggerBuilder.getBlogger());
  }

  /**
   * Use the BloggerUrlBuilder implementation to parse a Snapshot request into the appropriate URL
   * type.
   *
   * @param snapshotRequest The request data
   * @return A formatted Blogger URL
   * @throws MalformedURLException If the URL is invalid
   */
  private @NotNull URL getUrlFromRequest(@NotNull SnapshotRequest snapshotRequest)
      throws MalformedURLException {

    return bloggerBuilderFactory
        .getBloggerUrlBuilder(baseUrl)
        .labels(snapshotRequest.getLabels())
        .endDate(snapshotRequest.getEndDate())
        .startDate(snapshotRequest.getStartDate())
        .maxResults(snapshotRequest.getMaxResults())
        .pageToken(snapshotRequest.getPageToken())
        .fetchImages(snapshotRequest.isFetchImages())
        .fetchBodies(snapshotRequest.isFetchBodies())
        .orderBy(snapshotRequest.getOrderBy())
        .status(snapshotRequest.getStatus())
        .buildUrl();
  }
}
