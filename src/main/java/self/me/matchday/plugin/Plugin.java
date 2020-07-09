package self.me.matchday.plugin;

import java.util.UUID;

public interface Plugin {

  UUID getPluginId();

  String getTitle();

  String getDescription();

}
