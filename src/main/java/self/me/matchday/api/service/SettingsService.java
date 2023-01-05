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

package self.me.matchday.api.service;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import self.me.matchday.db.SettingsRepository;
import self.me.matchday.model.Settings;

@Service
public class SettingsService {

  private final Settings defaultSettings;
  private final SettingsRepository settingsRepository;
  private final ApplicationEventPublisher eventPublisher;
  public SettingsService(
      DefaultSettings defaultSettings,
      SettingsRepository settingsRepository,
      ApplicationEventPublisher eventPublisher) {
    this.defaultSettings = defaultSettings;
    this.settingsRepository = settingsRepository;
    this.eventPublisher = eventPublisher;
  }

  public Settings getSettings() {
    final List<Settings> settings = settingsRepository.findLatestSettings();
    if (settings.size() > 0) {
      return settings.get(0);
    }
    return defaultSettings;
  }

  public Settings updateSettings(@NotNull Settings settings) {
    final Settings updated = settingsRepository.save(settings);
    eventPublisher.publishEvent(new SettingsUpdatedEvent(this, updated));
    return updated;
  }

  public int deleteSettingsHistory(@Nullable Timestamp before) {
    if (before != null) {
      return settingsRepository.deleteAllByTimestampBefore(before);
    } else {
      final int count = settingsRepository.findAll().size();
      settingsRepository.deleteAll();
      return count;
    }
  }

  @Component
  @PropertySource("classpath:settings.default.properties")
  public static final class DefaultSettings extends Settings {

    @Value("${logging.file.name}")
    public void setLogFilename(String logFilename) {
      super.setLogFilename(Path.of(logFilename));
    }

    @Value("${artwork.storage-location}")
    public void setArtworkStorageLocation(String artworkStorageLocation) {
      super.setArtworkStorageLocation(Path.of(artworkStorageLocation));
    }

    @Value("${video-resources.file-storage-location}")
    public void setVideoStorageLocation(String videoStorageLocation) {
      super.setVideoStorageLocation(Path.of(videoStorageLocation));
    }

    @Value("${scheduled-tasks.cron.refresh-event-data}")
    public void setRefreshEventCron(String refreshEventCron) {
      super.setRefreshEvents(new CronTrigger(refreshEventCron));
    }

    @Value("${scheduled-tasks.cron.prune-video-data}")
    public void setPruneVideoCron(String pruneVideoCron) {
      super.setPruneVideos(new CronTrigger(pruneVideoCron));
    }

    @Override
    @Value("${scheduled-tasks.cron.video-data-expired-days}")
    public void setVideoExpiredDays(int videoExpiredDays) {
      super.setVideoExpiredDays(videoExpiredDays);
    }
  }

  public static final class SettingsUpdatedEvent extends ApplicationEvent {

    private final Settings settings;

    public SettingsUpdatedEvent(Object source, Settings settings) {
      super(source);
      this.settings = settings;
    }

    public Settings getSettings() {
      return settings;
    }
  }
}
