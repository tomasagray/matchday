package net.tomasbot.matchday.config.settings.plugin;

import java.nio.file.Path;
import java.util.List;
import net.tomasbot.matchday.model.Setting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class FFmpegBaseArgs implements Setting<List<String>> {

  public static final Path FFMPEG_BASE_ARGS = Path.of("/plugin/ffmpeg/ffmpeg/base-args");

  @Value("${plugin.ffmpeg.ffmpeg.base-args}")
  private List<String> baseArgs;

  @Value("${plugin.ffmpeg.protocols}")
  private List<String> protocols;

  @PostConstruct
  private void fixProtocols() {
    String collectedProtocols = String.join(",", protocols);
    baseArgs.add(collectedProtocols);
  }

  @Override
  public Path getPath() {
    return FFMPEG_BASE_ARGS;
  }

  @Override
  public List<String> getData() {
    return this.baseArgs;
  }
}
