package self.me.matchday.config.settings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import self.me.matchday.model.Setting;

import java.nio.file.Path;

@Component
public class VideoExpireDays implements Setting<Integer> {

    public static final Path VIDEO_EXPIRE_DAYS = Path.of("/tasks/video_expire_days");

    @Value("${scheduled-tasks.cron.video-data-expired-days}")
    private Integer videoExpiredDays;

    @Override
    public Path getPath() {
        return VIDEO_EXPIRE_DAYS;
    }

    @Override
    public Integer getData() {
        return this.videoExpiredDays;
    }
}
