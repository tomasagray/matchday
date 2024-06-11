/*
 * Copyright (c) 2023.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.tomasbot.matchday.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.SneakyThrows;
import net.tomasbot.matchday.model.ApplicationSettings;
import net.tomasbot.matchday.model.Setting;
import net.tomasbot.matchday.util.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

  @Getter private final ApplicationSettings settings = new ApplicationSettings();
  private final ApplicationEventPublisher eventPublisher;
  private final SettingsFileService fileService;

  public SettingsService(
      Collection<? extends Setting<?>> settings,
      ApplicationEventPublisher eventPublisher,
      SettingsFileService fileService) {
    this.eventPublisher = eventPublisher;
    this.fileService = fileService;
    this.settings.putAll(settings);
  }

  private static void validateSetting(@NotNull Setting<?> setting) {
    if (setting.getPath() == null) {
      throw new IllegalArgumentException("Setting path was null");
    }
  }

  @SuppressWarnings("unchecked cast")
  public <T> T getSetting(@NotNull Path path, @NotNull Class<T> type) {
    Setting<?> setting = settings.get(path);
    Object data = setting.getData();
    if (!type.isInstance(data)) {
      final String msg = String.format("Setting value <%s> is not of type %s", data, type);
      throw new IllegalArgumentException(msg);
    }
    return (T) data;
  }

  public ApplicationSettings updateSettings(@NotNull ApplicationSettings _settings)
      throws IOException, InterruptedException {
    Collection<? extends Setting<?>> settings = _settings.getAll();
    return updateSettings(settings);
  }

  public ApplicationSettings updateSettings(@NotNull Collection<? extends Setting<?>> settings)
      throws IOException, InterruptedException {
    for (Setting<?> setting : settings) {
      validateSetting(setting);
      this.settings.put(setting);
    }

    onSettingsUpdated();
    return getSettings();
  }

  public void updateSetting(@NotNull Setting<?> setting) throws IOException, InterruptedException {
    validateSetting(setting);
    this.settings.put(setting);
    onSettingsUpdated();
  }

  private void onSettingsUpdated() throws IOException, InterruptedException {
    eventPublisher.publishEvent(new SettingsUpdatedEvent(this));
    fileService.backupSettingsFile();
    fileService.writeSettingsFile(getSettings());
  }

  public int loadSettings() throws IOException, InterruptedException {
    ApplicationSettings settingsFile = fileService.readSettingsFile();
    Collection<? extends Setting<?>> settings = settingsFile.getAll();
    this.settings.putAll(settings);
    return settings.size();
  }

  @Getter
  public static final class SettingsUpdatedEvent extends ApplicationEvent {
    public SettingsUpdatedEvent(Object source) {
      super(source);
    }
  }

  @Service
  public static class SettingsFileService {

    public static final String SETTINGS_FILE = "settings.json";
    public static final String SETTINGS_BACKUP = "settings.backup.json";
    private static final Type TYPE = new TypeReference<ApplicationSettings>() {}.getType();

    private final Semaphore lock = new Semaphore(1, true);

    @Value("${application.config.root}")
    private Path configRoot;

    private static void writeData(@NotNull ApplicationSettings settings, @NotNull Path settingsFile)
        throws IOException {
      try (BufferedWriter writer =
          new BufferedWriter(new FileWriter(settingsFile.toFile(), false))) {
        final String json = JsonParser.toJson(settings, TYPE);
        writer.write(json);
      }
    }

    @Async
    public void writeSettingsFile(@NotNull ApplicationSettings settings)
        throws IOException, InterruptedException {
      try {
        lock.acquire();
        Path settingsFile = getSettingsFile();
        writeData(settings, settingsFile);
      } finally {
        lock.release();
      }
    }

    public ApplicationSettings readSettingsFile() throws IOException, InterruptedException {
      lock.acquire();
      Path settings = getSettingsFile();
      try (BufferedReader reader = new BufferedReader(new FileReader(settings.toFile()))) {
        String data = reader.lines().collect(Collectors.joining(""));
        return JsonParser.fromJson(data, TYPE);
      } finally {
        lock.release();
      }
    }

    public @NotNull Path getSettingsFile() {
      return configRoot.resolve(SETTINGS_FILE);
    }

    @SneakyThrows
    public void backupSettingsFile() {
      Path settingsFile = getSettingsFile();
      if (settingsFile.toFile().exists()) {
        ApplicationSettings settings = readSettingsFile();
        Path backup = configRoot.resolve(SETTINGS_BACKUP);
        writeData(settings, backup);
      }
    }
  }
}
