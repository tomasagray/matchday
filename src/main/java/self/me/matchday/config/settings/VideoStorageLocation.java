package self.me.matchday.config.settings;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import self.me.matchday.model.Setting;

@Component
public class VideoStorageLocation implements Setting<Path> {

  public static final Path VIDEO_STORAGE = Path.of("/filesystem/video_location");

  @Value("${video-resources.file-storage-location}")
  private Path videoStorage;

  @Override
  public Path getPath() {
    return VIDEO_STORAGE;
  }

  @Override
  public Path getData() {
    return videoStorage;
  }
}
