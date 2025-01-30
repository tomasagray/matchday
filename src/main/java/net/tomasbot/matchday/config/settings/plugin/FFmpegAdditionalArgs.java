package net.tomasbot.matchday.config.settings.plugin;

import java.nio.file.Path;
import java.util.List;
import net.tomasbot.matchday.model.Setting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FFmpegAdditionalArgs implements Setting<List<String>> {

  public static final Path FFMPEG_ADDITIONAL_ARGS =
      Path.of("/plugin/ffmpeg/ffmpeg/additional-args");

  @Value("${plugin.ffmpeg.ffmpeg.additional-args}")
  private List<String> additionalArgs;

  @Override
  public Path getPath() {
    return FFMPEG_ADDITIONAL_ARGS;
  }

  @Override
  public List<String> getData() {
    return this.additionalArgs;
  }
}
