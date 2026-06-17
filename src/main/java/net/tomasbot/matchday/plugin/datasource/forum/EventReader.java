package net.tomasbot.matchday.plugin.datasource.forum;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.tomasbot.matchday.api.service.EventService;
import net.tomasbot.matchday.model.DataSource;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.video.VideoFileSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EventReader {

  private static final Logger logger = LogManager.getLogger(EventReader.class);

  private final EventService eventService; // external dependency
  private final EventPageParser eventPageParser;
  private final FileSourceCorrectorMutator fileSourceCorrector;

  public EventReader(
      EventPageParser eventPageParser,
      EventService eventService,
      FileSourceCorrectorMutator fileSourceCorrector) {
    this.eventPageParser = eventPageParser;
    this.eventService = eventService;
    this.fileSourceCorrector = fileSourceCorrector;
  }

  public static void handleReadMetadataError(Throwable e, URI uri) {
    // see ForumPluginLog
  }

  public static void handleReadEventError(@NotNull Throwable e, @NotNull URL url) {
    // see ForumPluginLog
  }

  @Async("DataSourceRefresher")
  public CompletableFuture<Event> readListEvent(
      @NotNull Map.Entry<URI, ? extends Event> entry,
      @NotNull DataSource<? extends Event> dataSource) {
    URI uri = entry.getKey();
    Event event = entry.getValue();

    Optional<? extends Event> existingOptional = eventService.fetchEventLike(event);
    if (existingOptional.isEmpty()) {
      // this Event was not found in DB; it's new!
      try {
        // read remote data
        Event metadata = readEvent(uri.toURL(), dataSource);

        if (metadata != null) {
          Set<VideoFileSource> fileSources = metadata.getFileSources();
          correctFileSources(fileSources);
          event.getFileSources().addAll(fileSources);

          return CompletableFuture.completedFuture(event);
        }
      } catch (Throwable e) {
        handleReadMetadataError(e, uri);
      }
    }
    return null;
  }

  public Event readEvent(@NotNull URL url, @NotNull DataSource<? extends Event> dataSource) {
    try {
      // follow link
      String eventPage = RemoteDataReader.readDataFrom(url);

      // extract match metadata
      return eventPageParser.getEventFrom(dataSource, eventPage);
    } catch (IOException e) {
      handleReadEventError(e, url);
    }
    return null;
  }

  /**
   * Attempt to fill in missing metadata fields for the supplied VideoFileSources
   *
   * @param fileSources A Collection of
   */
  private void correctFileSources(@NotNull Collection<? extends VideoFileSource> fileSources) {
    for (VideoFileSource fileSource : fileSources)
      fileSourceCorrector.correctFileSource(fileSource);
  }
}
