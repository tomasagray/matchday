package self.me.matchday.config.settings;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import self.me.matchday.model.Setting;

@Component
public class RefreshDataSetting implements Setting<CronTrigger> {

  public static final Path REFRESH_DATASOURCES = Path.of("/tasks/refresh_datasources");

  @Value("${scheduled-tasks.cron.refresh-event-data}")
  private CronTrigger refreshData;

  @Override
  public Path getPath() {
    return REFRESH_DATASOURCES;
  }

  @Override
  public CronTrigger getData() {
    return this.refreshData;
  }
}
