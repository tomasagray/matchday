package self.me.matchday.config.settings;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import self.me.matchday.model.Setting;

@Component
public class EnabledFileServerPlugins implements Setting<Collection<UUID>> {

  public static final Path ENABLED_FILESERVERS = Path.of("/fileservers/enabled");
  private final Set<UUID> fileservers = new HashSet<>();

  @Override
  public Path getPath() {
    return ENABLED_FILESERVERS;
  }

  @Override
  public Collection<UUID> getData() {
    return this.fileservers;
  }
}
