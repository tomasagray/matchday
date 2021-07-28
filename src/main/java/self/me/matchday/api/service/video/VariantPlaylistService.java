/*
 * Copyright (c) 2021.
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

package self.me.matchday.api.service.video;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.api.service.EventFileService;
import self.me.matchday.api.service.EventService;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.video.M3UPlaylist;
import self.me.matchday.util.Log;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@Transactional
public class VariantPlaylistService {

  private static final String LOG_TAG = "VariantPlaylistService";

  private final EventService eventService;
  private final EventFileService eventFileService;

  @Autowired
  public VariantPlaylistService(
      final EventService eventService, final EventFileService eventFileService) {

    this.eventService = eventService;
    this.eventFileService = eventFileService;
  }

  /**
   * Retrieve an HLS playlist for the given file source (variant). Refreshes EventFile data, if
   * necessary.
   *
   * @param fileSrcId The ID of the file source from which to create the playlist
   * @return An Optional containing the playlist, if it was successfully created
   */
  public Optional<M3UPlaylist> fetchVariantPlaylist(final String fileSrcId) {

    Log.i(
        LOG_TAG, String.format("Fetching Variant Playlist for Event file source: %s ", fileSrcId));

    // Get Event
    final Optional<EventFileSource> fileSourceOptional = eventService.fetchEventFileSrc(fileSrcId);
    if (fileSourceOptional.isPresent()) {
      final EventFileSource eventFileSource = fileSourceOptional.get();
      if (eventFileSource.getEventFiles().size() > 0) {

        final M3UPlaylist playlist = new M3UPlaylist();
        // Refresh data for EventFiles & create playlist
        final List<M3UPlaylist> _playlist =
            eventFileSource.getEventFiles().stream()
                .map(
                    eventFile -> {
                      try {
                        return eventFileService.refreshEventFile(eventFile, false);
                      } catch (ExecutionException | InterruptedException e) {
                        final String msg = "Error refreshing EventFile: " + eventFile;
                        Log.i(LOG_TAG, msg, e);
                        throw new RuntimeException(msg, e);
                      }
                    })
                .map(
                    eventFile ->
                        playlist.addMediaSegment(
                            eventFile.getInternalUrl(), eventFile.getTitle().toString(), null))
                .collect(Collectors.toList());
        Log.i(LOG_TAG, "Created playlist: " + _playlist);

        return Optional.of(playlist);

      } else {
        Log.e(
            LOG_TAG,
            String.format(
                "Could not create variant playlist for EventFileSource: %s; no EventFiles!",
                eventFileSource));
      }
    } else {
      Log.e(
          LOG_TAG,
          String.format(
              "Could not create Variant Playlist; invalid EventFileSource ID: %s ", fileSrcId));
    }
    // Return optional
    return Optional.empty();
  }
}
