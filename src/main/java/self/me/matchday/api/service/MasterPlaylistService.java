package self.me.matchday.api.service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Service;
import self.me.matchday.api.controller.PlaylistController;
import self.me.matchday.db.EventRepository;
import self.me.matchday.db.EventSourceRepository;
import self.me.matchday.db.MasterM3URepository;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventSource;
import self.me.matchday.model.MasterM3U;
import self.me.matchday.util.Log;

@Service
public class MasterPlaylistService {

  private static final String LOG_TAG = "MasterPlaylistService";

  private final EventRepository eventRepository;
  private final MasterM3URepository masterM3URepository;
  private final EventSourceRepository eventSourceRepository;

  @Autowired
  MasterPlaylistService(EventRepository eventRepository, MasterM3URepository masterM3URepository,
      EventSourceRepository eventSourceRepository) {

    this.eventRepository = eventRepository;
    this.masterM3URepository = masterM3URepository;
    this.eventSourceRepository = eventSourceRepository;
  }

  /**
   * Get the Master playlist for the specified Event. If it does not exist yet in the database,
   * create it. If there are no EventSources or EventFileSources for this Event, the Optional which
   * is returned will be empty.
   *
   * @param eventId The ID of the Event for which we want a playlist.
   * @return An Optional containing the playlist.
   */
  public Optional<MasterM3U> fetchPlaylistById(@NotNull final String eventId) {

    Log.i(LOG_TAG, "Fetching Master Playlist for Event: " + eventId);
    Optional<MasterM3U> result = Optional.empty();
    // ensure valid Event ID
    if (eventRepository.findById(eventId).isPresent()) {
      // check DB for playlist
      final Optional<MasterM3U> playlistOptional = masterM3URepository.findByEventId(eventId);
      if (playlistOptional.isPresent()) {
        result = playlistOptional;
      } else {
        // if not found, create, save & return
        final Optional<MasterM3U> masterPlaylist = createMasterPlaylist(eventId);
        masterPlaylist.ifPresent(masterM3URepository::save);
        result = masterPlaylist;
      }
    }

    return result;
  }

  /**
   * Create a Master playlist (.m3u8) for the given Event.
   *
   * @param eventId The ID of the Event for which this playlist will be created.
   * @return An Optional containing the Master playlist.
   */
  private Optional<MasterM3U> createMasterPlaylist(@NotNull final String eventId) {

    Optional<MasterM3U> result = Optional.empty();

    // Get EventSources for this Event
    final Optional<List<EventSource>> eventSourceOptional =
        eventSourceRepository.findSourcesForEvent(eventId);
    if (eventSourceOptional.isPresent()) {

      // Collect all EventFileSources for this Event
      final List<EventFileSource> eventFileSources =
          eventSourceOptional
              .get().stream()
              .map(EventSource::getEventFileSources)
              .flatMap(Collection::stream)
              .collect(Collectors.toList());

      if (eventFileSources.size() > 0) {

        // Create Master Playlist
        final MasterM3U masterPlaylist = new MasterM3U(eventId);
        // Add variants
        eventFileSources.forEach(eventFileSource -> {
          // Get variant link
          final Link variantLink = linkTo(methodOn(PlaylistController.class)
              .fetchVariantPlaylist(eventId, eventFileSource.getEventFileSrcId())).withSelfRel();
          masterPlaylist.addVariant(eventFileSource, variantLink.toUri());
        });
        result = Optional.of(masterPlaylist);

      } else {
        Log.i(LOG_TAG, String
            .format("Did not generate playlist for Event with ID: %s; no file sources.", eventId));
      }
    } else {
      // Playlist generation failed
      Log.i(LOG_TAG, String
          .format("Could not generate Master Playlist for Event with ID: %s; no EventSources",
              eventId));
    }

    return result;
  }
}