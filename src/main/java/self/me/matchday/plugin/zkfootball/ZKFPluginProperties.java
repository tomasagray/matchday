package self.me.matchday.plugin.zkfootball;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import self.me.matchday.plugin.PluginProperties;

@Configuration
@PropertySource("classpath:plugins/zkf/zkf.properties")
@ConfigurationProperties(prefix = "plugin.zkf")
public class ZKFPluginProperties extends PluginProperties {

  private String baseUrl;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(final String baseUrl) {
    this.baseUrl = baseUrl;
  }
}
