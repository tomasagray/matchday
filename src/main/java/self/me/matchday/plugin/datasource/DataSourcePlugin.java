package self.me.matchday.plugin.datasource;

import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.Plugin;

public interface DataSourcePlugin<T> extends Plugin {

  Snapshot<T> getSnapshot(@NotNull final SnapshotRequest snapshotRequest) throws IOException;

}
