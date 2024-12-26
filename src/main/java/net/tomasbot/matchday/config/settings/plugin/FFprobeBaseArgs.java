package net.tomasbot.matchday.config.settings.plugin;

import java.nio.file.Path;
import java.util.List;

import net.tomasbot.matchday.model.Setting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FFprobeBaseArgs implements Setting<List<String>> {

  public static final Path FFPROBE_BASE_ARGS = Path.of("/plugin/ffprobe/ffprobe/base-args");

  @Value("${plugin.ffmpeg.ffprobe.base-args}")
  private List<String> baseArgs;

  @Override
  public Path getPath() {
    return FFPROBE_BASE_ARGS;
  }

  @Override
  public List<String> getData() {
    return this.baseArgs;
  }
}
