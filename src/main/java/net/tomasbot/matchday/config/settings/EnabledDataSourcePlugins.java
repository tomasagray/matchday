package net.tomasbot.matchday.config.settings;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.tomasbot.matchday.model.Setting;
import org.springframework.stereotype.Component;

@Component
public class EnabledDataSourcePlugins implements Setting<Set<UUID>> {

  public static final Path ENABLED_DATASOURCES = Path.of("/datasources/enabled");
  private final Set<UUID> dataSources = new HashSet<>();

  @Override
  public Path getPath() {
    return ENABLED_DATASOURCES;
  }

  @Override
  public Set<UUID> getData() {
    return this.dataSources;
  }
}
