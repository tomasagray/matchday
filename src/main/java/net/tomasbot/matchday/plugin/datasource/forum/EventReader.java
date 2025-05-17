package net.tomasbot.matchday.plugin.datasource.forum;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.tomasbot.matchday.api.service.EventService;
import net.tomasbot.matchday.model.DataSource;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.video.VideoFile;
import net.tomasbot.matchday.model.video.VideoFilePack;
import net.tomasbot.matchday.model.video.VideoFileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EventReader {

  private static final Pattern EXT_PATTERN = Pattern.compile("\\.(\\w+)$");

  private final EventPageParser eventPageParser;
  private final EventService eventService; // external dependency

  public EventReader(EventPageParser eventPageParser, EventService eventService) {
    this.eventPageParser = eventPageParser;
    this.eventService = eventService;
  }

  /**
   * Attempt to guess the media container (file extension) of the video files for this source from
   * one of the URLs of the supplied video source
   *
   * @param fileSource A video source
   * @return A trimmed, all-caps String representing the file extension, if it can be determined, or
   *     null if not
   */
  @Nullable
  private static String getExtensionFrom(@NotNull VideoFileSource fileSource) {
    return fileSource.getVideoFilePacks().stream()
        .map(VideoFilePack::allFiles)
        .flatMap(pack -> pack.values().stream())
        .map(VideoFile::getExternalUrl)
        .filter(Objects::nonNull)
        .findFirst()
        .map(
            url -> {
              Matcher matcher = EXT_PATTERN.matcher(url.toString());
              return matcher.find() ? matcher.group(1).trim().toUpperCase() : null;
            })
        .orElse(null);
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
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return null;
  }

  public Event readEvent(@NotNull URL url, @NotNull DataSource<? extends Event> dataSource)
      throws IOException {
    // follow link
    String eventPage = RemoteDataReader.readDataFrom(url);

    // extract match metadata
    return eventPageParser.getEventFrom(dataSource, eventPage);
  }

  /**
   * Attempt to fill-in missing metadata fields for the supplied VideoFileSources
   *
   * @param fileSources A Collection of
   */
  private void correctFileSources(@NotNull Collection<? extends VideoFileSource> fileSources) {
    for (VideoFileSource fileSource : fileSources) {
      String mediaContainer = fileSource.getMediaContainer();
      if (mediaContainer == null || mediaContainer.isEmpty()) {
        String extension = getExtensionFrom(fileSource);
        fileSource.setMediaContainer(extension);
      }
    }
  }
}
