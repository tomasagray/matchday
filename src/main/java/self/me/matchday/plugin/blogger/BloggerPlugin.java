package self.me.matchday.plugin.blogger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.UUID;
import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.DataSourcePlugin;
import self.me.matchday.plugin.blogger.parser.BloggerBuilder;
import self.me.matchday.plugin.blogger.parser.BloggerBuilderFactory;
import self.me.matchday.plugin.blogger.parser.html.HtmlBuilderFactory;
import self.me.matchday.plugin.blogger.parser.json.JsonBuilderFactory;
import self.me.matchday.util.Log;

@Data
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BloggerPlugin implements DataSourcePlugin<Blogger> {

  private static final String LOG_TAG = "BloggerPlugin";

  public enum FetchMode {
    JSON, HTML,
  }

  @Autowired
  private BloggerPluginProperties properties;
  private String baseUrl;
  private FetchMode fetchMode;
  private BloggerBuilderFactory bloggerBuilderFactory;

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
    final BloggerBuilder bloggerBuilder = bloggerBuilderFactory.getBloggerBuilder(url);
    // Create a Blogger Snapshot & return
    return
        new Snapshot<>(bloggerBuilder.getBlogger());
  }

  /**
   * Use the BloggerUrlBuilder implementation to parse a Snapshot request into the appropriate
   * URL type.
   *
   * @param snapshotRequest The request data
   * @return A formatted Blogger URL
   * @throws MalformedURLException If the URL is invalid
   */
  private @NotNull URL getUrlFromRequest(@NotNull SnapshotRequest snapshotRequest)
      throws MalformedURLException {

    return
        bloggerBuilderFactory
          .getBloggerUrlBuilder(baseUrl)
            .labels(Arrays.asList(snapshotRequest.getLabels()))
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
