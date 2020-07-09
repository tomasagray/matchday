package self.me.matchday.plugin;

import lombok.Data;

@Data
public abstract class PluginProperties {

  protected String id;
  protected String title;
  protected String description;

}
