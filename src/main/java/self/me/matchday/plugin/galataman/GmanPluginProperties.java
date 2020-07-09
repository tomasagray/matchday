package self.me.matchday.plugin.galataman;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import self.me.matchday.plugin.PluginProperties;

@Configuration
@PropertySource("classpath:plugins/gman/gman.properties")
@ConfigurationProperties(prefix = "plugin.gman")
public class GmanPluginProperties extends PluginProperties {

  private String baseUrl;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(final String baseUrl) {
    this.baseUrl = baseUrl;
  }
}
