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

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Service;
import self.me.matchday.api.controller.VideoStreamingController;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.MasterM3U;
import self.me.matchday.util.Log;

import java.util.Collection;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Service
public class MasterPlaylistService {

  private static final String LOG_TAG = "MasterPlaylistService";

  private final EventService eventService;

  @Autowired
  MasterPlaylistService(final EventService eventService) {
    this.eventService = eventService;
  }

  /**
   * Get the Master playlist for the specified Event. If it does not exist yet in the database,
   * create it. If there are no EventSources or EventFileSources for this Event, the Optional which
   * is returned will be empty.
   *
   * @param eventId The ID of the Event for which we want a playlist.
   * @return An Optional containing the playlist.
   */
  public Optional<MasterM3U> fetchMasterPlaylistForEvent(@NotNull final String eventId) {

    Log.i(LOG_TAG, "Fetching Master Playlist for Event: " + eventId);

    Optional<MasterM3U> result = Optional.empty();
    // ensure valid Event ID
    final Optional<Event> eventOptional = eventService.fetchById(eventId);
    if (eventOptional.isPresent()) {

      final Event event = eventOptional.get();
      // Create master playlist for this Event
      final MasterM3U masterPlaylist = createMasterPlaylist(event);
      if (masterPlaylist != null) {
        result = Optional.of(masterPlaylist);
      }
    }
    return result;
  }

  /**
   * Create a Master playlist (.m3u8) for the given Event.
   *
   * @param event The Event for which this playlist will be created.
   * @return An Optional containing the Master playlist.
   */
  private MasterM3U createMasterPlaylist(@NotNull final Event event) {

    // Get file sources for this event
    final Collection<EventFileSource> eventFileSources = event.getFileSources();
    if (eventFileSources.size() > 0) {
      // Create Master Playlist
      final MasterM3U masterPlaylist = new MasterM3U();
      // Add variants
      eventFileSources.forEach(
          eventFileSource -> {
            // Create variant link
            final Link variantLink =
                linkTo(
                        methodOn(VideoStreamingController.class)
                            .getVariantPlaylist(
                                event.getEventId(),
                                eventFileSource.getEventFileSrcId()))
                    .withSelfRel();
            // Add variant to master playlist
            masterPlaylist.addVariant(
                eventFileSource.getResolution(),
                eventFileSource.getLanguages(),
                eventFileSource.getBitrate(),
                variantLink.toUri());
          });

      return masterPlaylist;
    }
    // No variants - nothing to play
    return null;
  }
}
