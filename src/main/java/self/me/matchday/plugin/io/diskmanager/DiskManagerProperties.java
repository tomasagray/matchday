package self.me.matchday.plugin.io.diskmanager;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import self.me.matchday.plugin.PluginProperties;

@Configuration
@PropertySource("classpath:plugins/disk-manager/disk-manager.properties")
@ConfigurationProperties(prefix = "plugin.disk-manager")
public class DiskManagerProperties extends PluginProperties {

  private String storageLocation;

  public String getStorageLocation() {
    return storageLocation;
  }

  public void setStorageLocation(final String storageLocation) {
    this.storageLocation = storageLocation;
  }
}
