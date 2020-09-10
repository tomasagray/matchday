/*
 * Copyright (c) 2020.
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
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.DataSourceService;
import self.me.matchday.api.service.PlaylistLocatorService;
import self.me.matchday.api.service.VideoStreamingService;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.util.Log;

/**
 * Regularly run tasks. Configuration in external properties file.
 */
@Component
@PropertySource("classpath:scheduled-tasks.properties")
public class ScheduledTasks {

  private static final String LOG_TAG = "ScheduledTasks";

  private final DataSourceService dataSourceService;
  private final PlaylistLocatorService playlistLocatorService;
  private final VideoStreamingService videoStreamingService;

  @Value("${scheduled-tasks.cron.video-data-expired-days}")
  private int videoDataExpiredDays;

  @Autowired
  public ScheduledTasks(final DataSourceService dataSourceService,
      final PlaylistLocatorService playlistLocatorService,
      final VideoStreamingService videoStreamingService) {

    this.dataSourceService = dataSourceService;
    this.playlistLocatorService = playlistLocatorService;
    this.videoStreamingService = videoStreamingService;
  }

  @Scheduled(cron = "${scheduled-tasks.cron.refresh-event-data}")
  public void refreshEventData() {

    Log.i(LOG_TAG, "Refreshing all data sources...");
    // Create empty SnapshotRequest
    final SnapshotRequest snapshotRequest = SnapshotRequest.builder().build();
    // Refresh data sources
    dataSourceService.refreshDataSources(snapshotRequest);
  }

  @Scheduled(cron = "${scheduled-tasks.cron.prune-video-data}")
  public void pruneVideoData() {

    Log.i(LOG_TAG, "Pruning video data more than 2 weeks old...");

    // Examine each playlist & related data
    playlistLocatorService
        .getAllPlaylistLocators()
        .forEach(playlistLocator -> {
          try {

            // Read creation date of playlist file
            final BasicFileAttributes playlistFileAttributes =
                Files.readAttributes(playlistLocator.getPlaylistPath(), BasicFileAttributes.class);
            final Instant creationTime = playlistFileAttributes.creationTime().toInstant();

            final Duration sinceCreation = Duration.between(creationTime, Instant.now());
            final Duration expiredDays = Duration.ofDays(videoDataExpiredDays);

            if (sinceCreation.compareTo(expiredDays) > 0) {
              Log.i(LOG_TAG,
                  String.format("Video data is more than %s old; deleting for %s",
                      expiredDays, playlistLocator));

              // This video data is expired; delete it
              videoStreamingService.deleteVideoData(playlistLocator);
              // Delete playlist locator
              playlistLocatorService.deletePlaylistLocator(playlistLocator.getPlaylistId());
            }
          } catch (IOException e) {
            Log.e(LOG_TAG, "Error running scheduled delete of video data", e);
            // Wrap exception
            throw new RuntimeException(e);
          }
        });
  }
}
