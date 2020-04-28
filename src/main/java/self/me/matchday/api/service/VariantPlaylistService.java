package self.me.matchday.api.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

  // todo: de-couple refresh timeout? may be different for different file servers.
  private static final Duration REFRESH_RATE = Duration.ofHours(4);

  // JPA repositories
  private final EventRepository eventRepository;
  private final EventFileSrcRepository eventFileSrcRepository;
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
    Optional<VariantM3U> result = Optional.empty();

    // Get Event & EventFileSource
    final Optional<Event> eventOptional = eventRepository.findById(eventId);
    if (eventOptional.isPresent()) {
      final Event event = eventOptional.get();
      final Optional<EventFileSource> fileSourceOptional =
          eventFileSrcRepository.findFileSrcById(fileSrcId);
      if (fileSourceOptional.isPresent()) {
        final EventFileSource eventFileSource = fileSourceOptional.get();

        // Is the file data stale?
        if (shouldRefreshFileData(eventFileSource)) {
          eventFileService.refreshEventFileData(eventFileSource);
        }
        final List<EventFile> eventFiles = eventFileSource.getEventFiles();
        if(eventFiles.size() > 0) {
          result = Optional.of(new VariantM3U(event, eventFiles));
        } else {
          Log.e(LOG_TAG, "Could not create variant playlist; no EventFiles!");
        }
      } else {
        Log.e(LOG_TAG, "Could not create Variant Playlist; invalid File Source ID: " + fileSrcId);
      }
    } else {
      Log.e(LOG_TAG, "Could not create Variant Playlist; invalid Event ID: " + eventId);
    }

    return result;
  }

  private boolean shouldRefreshFileData(@NotNull final EventFileSource eventFileSource) {
    final Duration timeSinceRefresh = Duration
        .between(eventFileSource.getLastRefreshed().toInstant(), Instant.now()).abs();
    return timeSinceRefresh.toMillis() > REFRESH_RATE.toMillis();
  }
}
