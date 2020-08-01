package self.me.matchday.api.service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.VariantM3U;
import self.me.matchday.util.Log;

@Service
@Transactional
public class VariantPlaylistService {

  private static final String LOG_TAG = "VariantPlaylistService";

  private final EventService eventService;
  private final EventFileService eventFileService;

  @Autowired
  public VariantPlaylistService(EventService eventService, EventFileService eventFileService) {

    this.eventService = eventService;
    this.eventFileService = eventFileService;
  }

  public Optional<VariantM3U> fetchVariantPlaylist(@NotNull final String eventId,
      @NotNull final UUID fileSrcId) {

    Log.i(LOG_TAG, String
        .format("Fetching Variant Playlist for Event: %s, file source: %s ", eventId, fileSrcId));

    // Result container
    Optional<VariantM3U> result = Optional.empty();

    // Get Event
    final Optional<Event> eventOptional = eventService.fetchById(eventId);
    if (eventOptional.isPresent()) {

      final Event event = eventOptional.get();
      final EventFileSource eventFileSource = event.getFileSource(fileSrcId);
      if (eventFileSource.getEventFiles().size() > 0) {
        // Refresh data for EventFiles
        eventFileService.refreshEventFileData(eventFileSource, true);
        // Retrieve fresh EventFiles
        final Set<EventFile> eventFiles = eventFileSource.getEventFiles();
        // Create new Playlist & return
        result = Optional.of(new VariantM3U(event, eventFiles));

      } else {
        Log.e(LOG_TAG,
            String
                .format(
                    "Could not create variant playlist for EventFileSource: %s; no EventFiles!",
                    eventFileSource));
      }
    } else {
      Log.e(LOG_TAG,
          String.format("Could not create Variant Playlist; invalid Event ID: %s or "
              + "EventFileSource ID: %s ", eventId, fileSrcId));
    }
    // Return optional
    return result;
  }
}
