package self.me.matchday.plugin.io.ffmpeg;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import self.me.matchday.plugin.PluginProperties;


@Configuration
@PropertySource("classpath:plugins/ffmpeg/ffmpeg.properties")
@ConfigurationProperties(prefix = "plugin.ffmpeg")
public class FFmpegPluginProperties extends PluginProperties {

  private String ffmpegLocation;
  private String ffprobeLocation;

  public String getFFmpegLocation() {
    return ffmpegLocation;
  }

  public void setFFmpegLocation(String ffmpegLocation) {
    this.ffmpegLocation = ffmpegLocation;
  }

  public String getFFprobeLocation() {
    return ffprobeLocation;
  }

  public void setFFprobeLocation(String ffprobeLocation) {
    this.ffprobeLocation = ffprobeLocation;
  }
}
