package net.tomasbot.matchday.plugin.fileserver.filejoker;

import java.net.URL;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.tomasbot.matchday.plugin.PluginProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@Data
@EqualsAndHashCode(callSuper = true)
@Configuration
@PropertySource("classpath:plugins/filejoker/filejoker.properties")
@ConfigurationProperties(prefix = "plugin.filejoker")
public class FileJokerPluginProperties extends PluginProperties {

  private URL baseUrl;
  private URL loginUrl;
  private URL profileUrl;
  private String userAgent;
  private Pattern linkUrlPattern;
  private int refreshHours;
  private String bandwidthSelector;
  private String downloadSelector;
  private String directDownloadSelector;
}
