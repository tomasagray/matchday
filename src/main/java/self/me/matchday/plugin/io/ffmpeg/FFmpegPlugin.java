package self.me.matchday.plugin.io.ffmpeg;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.me.matchday.plugin.Plugin;

@Component
public class FFmpegPlugin implements Plugin {

  private final FFmpegPluginProperties pluginProperties;
  private final FFmpeg ffmpeg;
  private final FFprobe ffprobe;
  private final Executor executor;

  @Autowired
  public FFmpegPlugin(@NotNull final FFmpegPluginProperties pluginProperties) {

    this.pluginProperties = pluginProperties;
    // Create executable instances
    ffmpeg = new FFmpeg(pluginProperties.getFFmpegLocation());
    ffprobe = new FFprobe(pluginProperties.getFFprobeLocation());
    executor = Executors.newCachedThreadPool();
  }

  public Path streamUris(@NotNull final List<URI> uris, @NotNull final Path storageLocation) {

    final FFmpegTask streamTask = ffmpeg.getHlsStreamTask(uris, storageLocation);
    // Get playlist file location
    Path playlistFile = streamTask.getOutputFile();
    // TODO: Make this interrupt-able
    executor.execute(streamTask);
    return playlistFile;
  }

  @Override
  public UUID getPluginId() {
    return
        UUID.fromString(pluginProperties.getId());
  }

  @Override
  public String getTitle() {
    return pluginProperties.getTitle();
  }

  @Override
  public String getDescription() {
    return pluginProperties.getDescription();
  }
}
