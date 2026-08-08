package net.tomasbot.matchday.plugin.datasource.forum;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.tomasbot.matchday.api.service.EventService;
import net.tomasbot.matchday.common.HttpConnectionManager;
import net.tomasbot.matchday.common.flaresolverr.FlaresolverrService;
import net.tomasbot.matchday.common.flaresolverr.FlaresolverrSolution;
import net.tomasbot.matchday.model.DataSource;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.SecureCookie;
import net.tomasbot.matchday.model.video.VideoFileSource;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpCookie;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EventReader {
  
  private final EventService eventService; // external dependency
  private final EventPageParser eventPageParser;
  private final FileSourceCorrectorMutator fileSourceCorrector;
  private final FlaresolverrService flaresolverrService;
  private final HttpConnectionManager httpConnectionManager;

  public EventReader(
      EventPageParser eventPageParser,
      EventService eventService,
      FileSourceCorrectorMutator fileSourceCorrector,
      FlaresolverrService flaresolverrService,
      HttpConnectionManager httpConnectionManager) {
    this.eventPageParser = eventPageParser;
    this.eventService = eventService;
    this.fileSourceCorrector = fileSourceCorrector;
    this.flaresolverrService = flaresolverrService;
    this.httpConnectionManager = httpConnectionManager;
  }

  public static void handleReadMetadataError(Throwable ignore, URI uri) {
    // see ForumPluginLog
  }

  public static void handleReadEventError(@NotNull Throwable ignore, @NotNull URI uri) {
    // see ForumPluginLog
  }

  @Async("DataSourceRefresher")
  public CompletableFuture<Event> readListEvent(@NotNull EventMetaDataRequest request) {
    final Event event = request.getEvent();
    Optional<? extends Event> existingOptional = eventService.fetchEventLike(event);

    // The Event was not found in DB; it's new! We can proceed.
    // This check is done to limit reads of remote data
    if (existingOptional.isEmpty()) {
      // read remote data
      Event metadata = readEvent(request);
      // No metadata was read; return the Event as is
      if (metadata == null) return CompletableFuture.completedFuture(event);

      Set<VideoFileSource> fileSources = metadata.getFileSources();
      correctFileSources(fileSources);
      event.getFileSources().addAll(fileSources);
    }
    return CompletableFuture.completedFuture(event);
  }

  public Event readEvent(@NotNull EventMetaDataRequest request) {
    URI uri = request.getUri();
    String data;

    try {
      URL url = uri.toURL();
      DataSource<? extends Event> dataSource = request.getDataSource();
      Collection<SecureCookie> cookies = request.getCookies();

      if (dataSource.isFlared()) {
        if (cookies == null || cookies.isEmpty()) {
          // ask flaresolverr again...
          FlaresolverrSolution solution = flaresolverrService.solveChallengeFor(url);
          data = solution.getData();

          // add cookies for next request(s)
          request.setCookies(solution.getCookies());
        } else {
          // read with cookies
          Set<HttpCookie> httpCookies =
              cookies.stream().map(SecureCookie::toSpringCookie).collect(Collectors.toSet());
          data = httpConnectionManager.get(uri, httpCookies).bodyToMono(String.class).block();
        }
      } else
        data = httpConnectionManager.get(uri, new ArrayList<>()).bodyToMono(String.class).block();

      if (data == null || data.isBlank()) throw new IOException("Empty HTTP response");

      // extract match metadata
      return eventPageParser.getEventFrom(dataSource, data);
    } catch (IOException e) {
      handleReadEventError(e, uri);
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
