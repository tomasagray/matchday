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

import static net.tomasbot.matchday.config.settings.PruneVideos.PRUNE_VIDEOS;
import static net.tomasbot.matchday.config.settings.RefreshDataSetting.REFRESH_DATASOURCES;
import static net.tomasbot.matchday.config.settings.VideoExpireDays.VIDEO_EXPIRE_DAYS;
import static net.tomasbot.matchday.config.settings.VpnHeartbeat.VPN_HEARTBEAT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.PostConstruct;
import net.tomasbot.matchday.api.service.SettingsService.SettingsUpdatedEvent;
import net.tomasbot.matchday.api.service.admin.VpnService;
import net.tomasbot.matchday.api.service.video.VideoStreamLocatorPlaylistService;
import net.tomasbot.matchday.api.service.video.VideoStreamingService;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.SnapshotRequest;
import net.tomasbot.matchday.model.video.VideoStreamLocatorPlaylist;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/** Regularly run tasks. */
@Service
public class ScheduledTaskService {

  private final Map<TaskType, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

  private final TaskScheduler taskScheduler;
  private final SettingsService settingsService;
  private final DataSourceService dataSourceService;
  private final VideoStreamLocatorPlaylistService streamPlaylistService;
  private final VideoStreamingService videoStreamingService;
  private final EventService eventService;
  private final VpnService vpnService;

  public ScheduledTaskService(
      DataSourceService dataSourceService,
      VideoStreamLocatorPlaylistService streamPlaylistService,
      VideoStreamingService videoStreamingService,
      EventService eventService,
      TaskScheduler taskScheduler,
      SettingsService settingsService,
      VpnService vpnService) {
    this.dataSourceService = dataSourceService;
    this.streamPlaylistService = streamPlaylistService;
    this.videoStreamingService = videoStreamingService;
    this.eventService = eventService;
    this.taskScheduler = taskScheduler;
    this.settingsService = settingsService;
    this.vpnService = vpnService;
  }

  @PostConstruct
  private void initialTaskSchedule() {
    if (settingsService != null) {
      scheduleTasks();
    }
  }

  public void rescheduleTasks() {
    // cancel already running tasks
    scheduledTasks.forEach((type, task) -> task.cancel(false));
    scheduleTasks();
  }

  public void scheduleTasks() {
    CronTrigger refreshEventsSetting =
        settingsService.getSetting(REFRESH_DATASOURCES, CronTrigger.class);
    CronTrigger pruneVideoDataSetting = settingsService.getSetting(PRUNE_VIDEOS, CronTrigger.class);
    CronTrigger vpnHeartbeatSetting = settingsService.getSetting(VPN_HEARTBEAT, CronTrigger.class);

    ScheduledFuture<?> refreshEvents =
        taskScheduler.schedule(this::refreshEventData, refreshEventsSetting);
    ScheduledFuture<?> pruneVideoData =
        taskScheduler.schedule(this::pruneVideoData, pruneVideoDataSetting);
    ScheduledFuture<?> testVpnHeartbeat =
        taskScheduler.schedule(this::testVpnHeartbeat, vpnHeartbeatSetting);

    scheduledTasks.put(TaskType.REFRESH_EVENTS, refreshEvents);
    scheduledTasks.put(TaskType.PRUNE_VIDEOS, pruneVideoData);
    scheduledTasks.put(TaskType.VPN_HEARTBEAT, testVpnHeartbeat);
  }

  private boolean isVideoDataStale(@NotNull VideoStreamLocatorPlaylist playlist) {
    int expiredDays = settingsService.getSetting(VIDEO_EXPIRE_DAYS, Integer.class);
    final Instant creationTime = playlist.getTimestamp();
    final Duration sinceCreation = Duration.between(creationTime, Instant.now());
    final Duration expiry = Duration.ofDays(expiredDays);
    return sinceCreation.compareTo(expiry) > 0;
  }

  public void refreshEventData() {
    try {
      final LocalDateTime startDate = getLatestEventDate();
      final SnapshotRequest snapshotRequest =
          SnapshotRequest.builder().startDate(startDate).build();
      dataSourceService.refreshAllDataSources(snapshotRequest);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private LocalDateTime getLatestEventDate() {
    final int eventCount = 1;
    return eventService.fetchAllPaged(0, eventCount).stream()
        .map(Event::getDate)
        .findFirst()
        .orElse(null);
  }

  public void pruneVideoData() {
    try {
      List<VideoStreamLocatorPlaylist> playlists =
          streamPlaylistService.getAllVideoStreamPlaylists();
      for (VideoStreamLocatorPlaylist playlist : playlists) {
        if (isVideoDataStale(playlist)) {
          videoStreamingService.deleteAllVideoData(playlist);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void testVpnHeartbeat() {
    vpnService.doHeartbeat();
  }

  private enum TaskType {
    REFRESH_EVENTS,
    PRUNE_VIDEOS,
    VPN_HEARTBEAT,
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
      taskService.rescheduleTasks();
    }
  }
}
