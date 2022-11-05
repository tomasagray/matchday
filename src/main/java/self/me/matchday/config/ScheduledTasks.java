/*
 * Copyright (c) 2022.
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

package self.me.matchday.config;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.DataSourceService;
import self.me.matchday.api.service.EventService;
import self.me.matchday.api.service.video.VideoStreamLocatorPlaylistService;
import self.me.matchday.api.service.video.VideoStreamingService;
import self.me.matchday.model.Event;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;

/** Regularly run tasks. Configuration in external properties file. */
@Component
@PropertySource("classpath:scheduled-tasks.properties")
public class ScheduledTasks {

  private final DataSourceService dataSourceService;
  private final VideoStreamLocatorPlaylistService streamPlaylistService;
  private final VideoStreamingService videoStreamingService;
  private final EventService eventService;

  @Value("${scheduled-tasks.cron.video-data-expired-days}")
  private int videoDataExpiredDays;

  @Autowired
  public ScheduledTasks(
      DataSourceService dataSourceService,
      VideoStreamLocatorPlaylistService streamPlaylistService,
      VideoStreamingService videoStreamingService,
      EventService eventService) {

    this.dataSourceService = dataSourceService;
    this.streamPlaylistService = streamPlaylistService;
    this.videoStreamingService = videoStreamingService;
    this.eventService = eventService;
  }

  @Scheduled(cron = "${scheduled-tasks.cron.refresh-event-data}")
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

  @Scheduled(cron = "${scheduled-tasks.cron.prune-video-data}")
  public void pruneVideoData() throws IOException {
    List<VideoStreamLocatorPlaylist> playlists = streamPlaylistService.getAllVideoStreamPlaylists();
    for (VideoStreamLocatorPlaylist playlist : playlists) {
      if (videoDataIsStale(playlist)) {
        videoStreamingService.deleteVideoData(playlist);
      }
    }
  }

  private boolean videoDataIsStale(@NotNull VideoStreamLocatorPlaylist playlist) {
    final Instant creationTime = playlist.getTimestamp();
    final Duration sinceCreation = Duration.between(creationTime, Instant.now());
    final Duration expiredDays = Duration.ofDays(videoDataExpiredDays);
    return sinceCreation.compareTo(expiredDays) > 0;
  }
}
