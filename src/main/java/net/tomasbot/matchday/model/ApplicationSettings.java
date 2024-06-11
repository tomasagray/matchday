package net.tomasbot.matchday.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.api.controller.converter.ApplicationSettingsDeserializer;
import net.tomasbot.matchday.api.controller.converter.ApplicationSettingsSerializer;

@ToString
@JsonSerialize(using = ApplicationSettingsSerializer.class)
@JsonDeserialize(using = ApplicationSettingsDeserializer.class)
public class ApplicationSettings {

  private final Map<Path, Setting<?>> settings = new HashMap<>();

  public Setting<?> get(@NotNull Path path) {
    return settings.get(path);
  }

  public Collection<? extends Setting<?>> getAll() {
    return settings.values();
  }

  public void put(@NotNull Setting<?> setting) {
    settings.put(setting.getPath(), setting);
  }

  public void putAll(@NotNull Collection<? extends Setting<?>> settings) {
    for (Setting<?> setting : settings) {
      put(setting);
    }
  }

  public int size() {
    return settings.size();
  }
}
