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

package self.me.matchday.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.VariantM3U;
import self.me.matchday.util.Log;

import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VariantPlaylistService {

  private static final String LOG_TAG = "VariantPlaylistService";

  private final EventService eventService;
  private final EventFileService eventFileService;

  @Autowired
  public VariantPlaylistService(final EventService eventService,
      final EventFileService eventFileService) {

    this.eventService = eventService;
    this.eventFileService = eventFileService;
  }

  public Optional<VariantM3U> fetchVariantPlaylist(final String fileSrcId) {

    Log.i(LOG_TAG, String
        .format("Fetching Variant Playlist for Event file source: %s ", fileSrcId));

    // Result container
    Optional<VariantM3U> result = Optional.empty();

    // Get Event
    final Optional<EventFileSource> eventOptional = eventService.fetchEventFileSrc(fileSrcId);
    if (eventOptional.isPresent()) {

      final EventFileSource eventFileSource = eventOptional.get();
      if (eventFileSource.getEventFiles().size() > 0) {

        // Refresh data for EventFiles
        eventFileService.refreshEventFileData(eventFileSource, true);
        // Retrieve fresh EventFiles & sort
        final List<EventFile> eventFiles = eventFileSource.getEventFiles();
        Collections.sort(eventFiles);
        // Create new Playlist & return
        result = Optional.of(new VariantM3U(eventFiles));

      } else {
        Log.e(LOG_TAG,
            String
                .format(
                    "Could not create variant playlist for EventFileSource: %s; no EventFiles!",
                    eventFileSource));
      }
    } else {
      Log.e(LOG_TAG,
          String.format("Could not create Variant Playlist; invalid EventFileSource ID: %s ",
              fileSrcId));
    }
    // Return optional
    return result;
  }
}
