package self.me.matchday.api.service;

import java.util.Optional;
import java.util.Set;
import javax.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.db.EventFileSrcRepository;
import self.me.matchday.db.EventRepository;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.VariantM3U;
import self.me.matchday.util.Log;

@Service
@Transactional
public class VariantPlaylistService {

  private static final String LOG_TAG = "VariantPlaylistService";

  // JPA repositories
  private final EventRepository eventRepository;
  private final EventFileSrcRepository eventFileSrcRepository;
  // Services
  private final EventFileService eventFileService;

  @Autowired
  public VariantPlaylistService(EventRepository eventRepository,
      EventFileSrcRepository eventFileSrcRepository, EventFileService eventFileService) {

    this.eventRepository = eventRepository;
    this.eventFileSrcRepository = eventFileSrcRepository;
    this.eventFileService = eventFileService;
  }

  public Optional<VariantM3U> fetchVariantPlaylist(@NotNull final String eventId,
      @NotNull final Long fileSrcId) {

    Log.i(LOG_TAG, String
        .format("Fetching Variant Playlist for Event: %s, file source: %s ", eventId, fileSrcId));

    // Result container
    Optional<VariantM3U> result = Optional.empty();

    // Get Event
    final Optional<Event> eventOptional = eventRepository.findById(eventId);
    // Get EventFileSource
    final Optional<EventFileSource> fileSourceOptional = eventFileSrcRepository.findById(fileSrcId);
    // Proceed only if all data is present
    if (eventOptional.isPresent() && fileSourceOptional.isPresent()) {
      // Get data
      final Event event = eventOptional.get();
      final EventFileSource eventFileSource = fileSourceOptional.get();
      // Refresh data for EventFiles
      eventFileService.refreshEventFileData(eventFileSource);
      // Retrieve fresh EventFiles
      final Set<EventFile> eventFiles = eventFileSource.getEventFiles();

      // Create new Playlist & return
      if (eventFiles.size() > 0) {
        result = Optional.of(new VariantM3U(event, eventFiles));

      } else {
        Log.e(LOG_TAG,
            String.format("Could not create variant playlist for EventFileSource: %s; no EventFiles!",
                    eventFileSource));
      }
    } else {
      Log.e(LOG_TAG,
          String.format("Could not create Variant Playlist; invalid Event ID: %s or "
              + "EventFileSource ID: %s ", eventId, fileSrcId)
          + eventId);
    }
    // Return optional
    return result;
  }
}
