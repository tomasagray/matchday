package net.tomasbot.matchday.config.settings;

import java.nio.file.Path;
import net.tomasbot.matchday.model.Setting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
