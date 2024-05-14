package self.me.matchday.config.settings;

import org.springframework.stereotype.Component;
import self.me.matchday.model.Setting;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
