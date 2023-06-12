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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import self.me.matchday.api.service.SettingsService.SettingsUpdatedEvent;
import self.me.matchday.api.service.video.VideoStreamLocatorPlaylistService;
import self.me.matchday.api.service.video.VideoStreamingService;
import self.me.matchday.model.Event;
import self.me.matchday.model.Settings;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;

/** Regularly run tasks. */
@Service
public class ScheduledTaskService {

  private final SettingsService settingsService;
  private final TaskScheduler taskScheduler;

  private final DataSourceService dataSourceService;
  private final VideoStreamLocatorPlaylistService streamPlaylistService;
  private final VideoStreamingService videoStreamingService;
  private final EventService eventService;
  private final Map<TaskType, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

  public ScheduledTaskService(
      DataSourceService dataSourceService,
      VideoStreamLocatorPlaylistService streamPlaylistService,
      VideoStreamingService videoStreamingService,
      EventService eventService,
      TaskScheduler taskScheduler,
      SettingsService settingsService) {

    this.dataSourceService = dataSourceService;
    this.streamPlaylistService = streamPlaylistService;
    this.videoStreamingService = videoStreamingService;
    this.eventService = eventService;
    this.taskScheduler = taskScheduler;
    this.settingsService = settingsService;
    if (settingsService != null) {
      scheduleTasks(settingsService.getSettings());
    }
  }

  public void rescheduleTask(Settings settings) {
    // cancel already running tasks
    final ScheduledFuture<?> refreshEventsTask = scheduledTasks.get(TaskType.REFRESH_EVENTS);
    if (refreshEventsTask != null) {
      refreshEventsTask.cancel(false);
    }
    final ScheduledFuture<?> pruneTask = scheduledTasks.get(TaskType.PRUNE_VIDEOS);
    if (pruneTask != null) {
      pruneTask.cancel(false);
    }
    scheduleTasks(settings);
  }

  public void scheduleTasks(@NotNull Settings settings) {
    scheduledTasks.put(
        TaskType.REFRESH_EVENTS,
        taskScheduler.schedule(
            () -> {
              try {
                refreshEventData();
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            },
            settings.getRefreshEvents()));
    scheduledTasks.put(
        TaskType.PRUNE_VIDEOS,
        taskScheduler.schedule(
            () -> {
              try {
                pruneVideoData();
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            },
            settings.getPruneVideos()));
  }

  private boolean videoDataIsStale(@NotNull VideoStreamLocatorPlaylist playlist) {
    final Instant creationTime = playlist.getTimestamp();
    final Duration sinceCreation = Duration.between(creationTime, Instant.now());
    final Duration expiredDays =
        Duration.ofDays(settingsService.getSettings().getVideoExpiredDays());
    return sinceCreation.compareTo(expiredDays) > 0;
  }

  public void refreshEventData() throws IOException {
    // find latest Events (date sorted)
    final List<Event> events = eventService.fetchAll();
    LocalDateTime endDate = null;
    if (events.size() > 0) {
      final Event latest = events.get(0);
      endDate = latest.getDate();
    }
    final SnapshotRequest snapshotRequest = SnapshotRequest.builder().endDate(endDate).build();
    dataSourceService.refreshAllDataSources(snapshotRequest);
  }

  public void pruneVideoData() throws IOException {
    List<VideoStreamLocatorPlaylist> playlists = streamPlaylistService.getAllVideoStreamPlaylists();
    for (VideoStreamLocatorPlaylist playlist : playlists) {
      if (videoDataIsStale(playlist)) {
        videoStreamingService.deleteAllVideoData(playlist);
      }
    }
  }

  private enum TaskType {
    REFRESH_EVENTS,
    PRUNE_VIDEOS,
  }

  @Component
  public static final class SettingsUpdatedListener
      implements ApplicationListener<SettingsUpdatedEvent> {

    private final ScheduledTaskService taskService;

    public SettingsUpdatedListener(ScheduledTaskService taskService) {
      this.taskService = taskService;
    }

    @Override
    @Contract(pure = true)
    public void onApplicationEvent(@NotNull SettingsUpdatedEvent event) {
      final Settings settings = event.getSettings();
      taskService.rescheduleTask(settings);
    }
  }
}
