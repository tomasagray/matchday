package self.me.matchday.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.DataSourceService;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.util.Log;

/**
 * Regularly run tasks. Configuration in external properties file.
 */
@Component
@PropertySource("classpath:scheduled-tasks.properties")
public class ScheduledTasks {

  private static final String LOG_TAG = "ScheduledTasks";

  private final DataSourceService dataSourceService;

  @Autowired
  public ScheduledTasks(@NotNull final DataSourceService dataSourceService) {
    this.dataSourceService = dataSourceService;
  }

  @Scheduled(cron = "${scheduled-tasks.cron.refresh-event-data}")
  public void refreshEventData() {

    Log.i(LOG_TAG, "Refreshing all data sources...");
    // Create empty SnapshotRequest
    final SnapshotRequest snapshotRequest = SnapshotRequest.builder().build();
    // Refresh data sources
    dataSourceService.refreshDataSources(snapshotRequest);
  }
}
