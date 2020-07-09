package self.me.matchday.plugin.galataman;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.me.matchday.plugin.blogger.BloggerParserPlugin;
import self.me.matchday.plugin.blogger.BloggerPlugin;
import self.me.matchday.plugin.blogger.BloggerPlugin.FetchMode;
import self.me.matchday.model.Event;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.util.Log;

@Component
public class GManPlugin extends BloggerParserPlugin {

  private static final String LOG_TAG = "GManPlugin";

  private final GmanPluginProperties pluginProperties;

  @Autowired
  public GManPlugin(@NotNull final BloggerPlugin bloggerPlugin,
      @NotNull final GmanPluginProperties pluginProperties) {

    // Instantiate plugin with correct post parser
    super(bloggerPlugin, new GalatamanParserFactory());
    this.pluginProperties = pluginProperties;
    // Setup Blogger plugin
    this.bloggerPlugin.setBaseUrl(this.pluginProperties.getBaseUrl());
    this.bloggerPlugin.setFetchMode(FetchMode.HTML);
  }

  @Override
  public UUID getPluginId() {
    return
        UUID.fromString(pluginProperties.getId());
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

    Log.i(LOG_TAG,
        String.format("Refreshing Galataman HDF [@ %s] data with Snapshot:\n%s\n",
            pluginProperties.getBaseUrl(), snapshotRequest));
    return
        bloggerPlugin
            .getSnapshot(snapshotRequest)
            .map(this::getEventStream);
  }
}
