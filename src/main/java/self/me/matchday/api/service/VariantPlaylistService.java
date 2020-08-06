package self.me.matchday.api.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
  public VariantPlaylistService(final EventService eventService,
      final EventFileService eventFileService) {

    this.eventService = eventService;
    this.eventFileService = eventFileService;
  }

  public Optional<VariantM3U> fetchVariantPlaylist(@NotNull final UUID fileSrcId) {

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
        // Retrieve fresh EventFiles
        final List<EventFile> eventFiles = eventFileSource.getEventFiles();
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
