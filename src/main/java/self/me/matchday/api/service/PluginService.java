package self.me.matchday.api.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.matchday.model.Setting;
import self.me.matchday.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class PluginService {

    private final SettingsService settingsService;

    public PluginService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void enablePlugin(@NotNull Plugin plugin, Path settingPath) {
        try {
            Set<UUID> enabled = getIds(settingPath);
            enabled.add(plugin.getPluginId());
            settingsService.updateSetting(new Setting.GenericSetting<>(settingPath, enabled));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void disablePlugin(@NotNull Plugin plugin, Path settingPath) {
        try {
            Set<UUID> enabled = getIds(settingPath);
            enabled.remove(plugin.getPluginId());
            settingsService.updateSetting(new Setting.GenericSetting<>(settingPath, enabled));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isPluginEnabled(@NotNull Plugin plugin, @NotNull Path settingPath) {
        Set<UUID> enabled = getIds(settingPath);
        return enabled.contains(plugin.getPluginId());
    }

    private @NotNull Set<UUID> getIds(@NotNull Path settingPath) {
        Collection<?> settings = settingsService.getSetting(settingPath, Collection.class);
        Set<UUID> ids = new HashSet<>();
        for (Object o : settings) {
            ids.add(UUID.fromString(o.toString()));
        }
        return ids;
    }
}
