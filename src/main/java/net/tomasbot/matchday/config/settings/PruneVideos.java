package net.tomasbot.matchday.config.settings;

import java.nio.file.Path;
import net.tomasbot.matchday.model.Setting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Component
public class PruneVideos implements Setting<CronTrigger> {

  public static final Path PRUNE_VIDEOS = Path.of("/tasks/prune_videos");

  @Value("${scheduled-tasks.cron.prune-video-data}")
  private CronTrigger pruneVideos;

  @Override
  public Path getPath() {
    return PRUNE_VIDEOS;
  }

  @Override
  public CronTrigger getData() {
    return this.pruneVideos;
  }
}
