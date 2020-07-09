package self.me.matchday.plugin.zkfootball;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.me.matchday.model.Event;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.blogger.BloggerParserPlugin;
import self.me.matchday.plugin.blogger.BloggerPlugin;
import self.me.matchday.plugin.blogger.BloggerPlugin.FetchMode;
import self.me.matchday.util.Log;

@Component
public class ZKFPlugin extends BloggerParserPlugin {

  private static final String LOG_TAG = "ZKFPlugin";

  private final ZKFPluginProperties pluginProperties;

  @Autowired
  public ZKFPlugin(@NotNull final BloggerPlugin bloggerPlugin,
      @NotNull final ZKFPluginProperties pluginProperties) {

    // Instantiate plugin with correct post parser factory
    super(bloggerPlugin, new ZKFParserFactory());
    this.pluginProperties = pluginProperties;
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

    Log.i(LOG_TAG, String.format("Refreshing ZK Football [@ %s] data with Snapshot:\n%s\n",
        pluginProperties.getBaseUrl(), snapshotRequest));
    return
        bloggerPlugin
            .getSnapshot(snapshotRequest)
            .map(this::getEventStream);
  }
}
